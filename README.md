# order-notification-lambda

An AWS Lambda function written in **Java 21** that listens to an **AWS SQS** queue and sends **Email (SES)** + **SMS (SNS)** notifications to customers when an order is placed.

Triggered by [spring-order-service](https://github.com/voorevamshi/spring-order-service) — which publishes an order event to SQS after saving the order to the database.

---

## Architecture

```
spring-order-service
        │
        │  publishes JSON event
        ▼
  AWS SQS Queue
(order-notification-queue)
        │
        │  triggers automatically
        ▼
AWS Lambda — Java 21
(OrderNotificationHandler)
        │
        ├─────────────────────┐
        ▼                     ▼
   AWS SES               AWS SNS
(Email to customer)   (SMS to customer)
```

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| AWS Lambda Java Core | 1.2.3 | Lambda handler interface |
| AWS Lambda Java Events | 3.11.4 | SQS event model |
| AWS SDK v1 SES | 1.12.720 | Send emails |
| AWS SDK v1 SNS | 1.12.720 | Send SMS |
| Jackson | 2.17.0 | JSON deserialization |
| SLF4J Simple | 2.0.13 | Lightweight logging |
| Maven | 3.x | Build tool |

> **Why AWS SDK v1?** SDK v2 pulls in Netty (async HTTP client) which adds ~8MB. Lambda doesn't need async HTTP — SDK v1 uses a simple HTTP client and keeps the JAR size under 1MB.

---

## Project Structure

```
order-notification-lambda/
├── pom.xml
└── src/
    ├── assembly/
    │   └── layer.xml                        # Packages dependencies into layer.zip
    └── main/java/com/example/
        ├── handler/
        │   └── OrderNotificationHandler.java  # Lambda entry point (triggered by SQS)
        ├── service/
        │   ├── NotificationService.java       # Orchestrates Email + SMS
        │   ├── EmailService.java              # Sends email via AWS SES
        │   └── SmsService.java               # Sends SMS via AWS SNS
        └── model/
            ├── OrderEvent.java               # Deserialized from SQS message body
            └── NotificationResult.java       # Tracks email/SMS success per order
```

---

## Prerequisites

- Java 21
- Maven 3.x
- AWS Account with:
  - SQS queue: `order-notification-queue`
  - SES: sender email verified (e.g. `noreply@mystore.com`)
  - SNS: phone numbers verified (sandbox) or production access requested
- AWS credentials configured locally

---

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-username/order-notification-lambda.git
cd order-notification-lambda
```

### 2. Configure AWS credentials

Create `C:\Users\<your-user>\.aws\credentials` (Windows) or `~/.aws/credentials` (Mac/Linux):

```ini
[default]
aws_access_key_id     = YOUR_ACCESS_KEY_ID
aws_secret_access_key = YOUR_SECRET_ACCESS_KEY
```

Create `C:\Users\<your-user>\.aws\config` (Windows) or `~/.aws/config` (Mac/Linux):

```ini
[default]
region = ap-south-1
```

### 3. Build the project

```bash
mvn clean package
```

This produces **two files** in `target/`:

```
target/
├── order-notification-lambda-1.0.0.jar   ← ~50KB  your code only
└── layer.zip                             ← ~10MB  all dependencies
```

---

## Build Output Explained

| File | Size | Contains | Upload when |
|---|---|---|---|
| `order-notification-lambda-1.0.0.jar` | ~50KB | Your Java classes only | Every code change |
| `layer.zip` | ~10MB | All dependency JARs | Once, or when deps change |

This split keeps deployments fast — you only re-upload 50KB when you change business logic instead of 11MB every time.

---

## AWS Deployment

### Step 1 — Create the Lambda Layer (once)

**Via AWS Console:**
```
AWS Console → Lambda → Layers → Create Layer
  Name:                 order-notification-deps
  Upload:               target/layer.zip
  Compatible runtimes:  Java 21
→ Create
```
Note the **Layer ARN** shown after creation.

**Via AWS CLI:**
```bash
aws lambda publish-layer-version ^
  --layer-name order-notification-deps ^
  --zip-file fileb://target/layer.zip ^
  --compatible-runtimes java21
```

---

### Step 2 — Create the Lambda Function

**Via AWS Console:**
```
AWS Console → Lambda → Create Function
  Name:     order-notification-lambda
  Runtime:  Java 21
  Handler:  com.example.handler.OrderNotificationHandler::handleRequest
  Memory:   512 MB
  Timeout:  30 seconds
```

**Via AWS CLI:**
```bash
aws lambda create-function ^
  --function-name order-notification-lambda ^
  --runtime java21 ^
  --handler com.example.handler.OrderNotificationHandler::handleRequest ^
  --role arn:aws:iam::YOUR_ACCOUNT_ID:role/lambda-execution-role ^
  --memory-size 512 ^
  --timeout 30 ^
  --zip-file fileb://target/order-notification-lambda-1.0.0.jar
```

---

### Step 3 — Attach the Layer

```
AWS Console → Lambda → order-notification-lambda
  → Code tab → Layers → Add a layer
  → Custom layers → select order-notification-deps → version 1
→ Add
```

---

### Step 4 — Add SQS Trigger

```
AWS Console → Lambda → order-notification-lambda
  → Add trigger → SQS
  → Queue:      order-notification-queue
  → Batch size: 10
→ Add
```

---

### Step 5 — Set Environment Variables

```
AWS Console → Lambda → order-notification-lambda
  → Configuration → Environment variables → Edit
  → Add:
      SENDER_EMAIL   = noreply@mystore.com
      SMS_SENDER_ID  = MYSTORE
```

---

### Step 6 — Attach IAM Permissions

Attach this policy to the Lambda execution role:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:ap-south-1:YOUR_ACCOUNT_ID:order-notification-queue"
    },
    {
      "Effect": "Allow",
      "Action": "ses:SendEmail",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": "sns:Publish",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

---

## Day-to-Day Deployment

```bash
# Code changed → rebuild and upload only the thin JAR (50KB)
mvn clean package

aws lambda update-function-code ^
  --function-name order-notification-lambda ^
  --zip-file fileb://target/order-notification-lambda-1.0.0.jar

# Added a new dependency → rebuild and upload a new layer version (rare)
mvn clean package

aws lambda publish-layer-version ^
  --layer-name order-notification-deps ^
  --zip-file fileb://target/layer.zip ^
  --compatible-runtimes java21
# Then attach the new layer version in the console
```

---

## Lambda Configuration Reference

| Setting | Value | Reason |
|---|---|---|
| Runtime | Java 21 | Latest LTS |
| Handler | `com.example.handler.OrderNotificationHandler::handleRequest` | Entry point |
| Memory | 512 MB | Image processing + SDK clients need headroom |
| Timeout | 30 sec | SES + SNS calls are fast but allow buffer |
| Trigger | SQS | Auto-triggered on new messages |
| Batch size | 10 | Processes up to 10 orders per invocation |

---

## SQS Message Format

The Lambda expects this JSON in each SQS message body (published by `spring-order-service`):

```json
{
  "orderId": "be79b3d2-f08a-4cad-bb30-ce53ed9fe4b3",
  "customerId": "CUST-001",
  "customerName": "Ravi Kumar",
  "customerEmail": "ravi.kumar@gmail.com",
  "customerPhone": "+919876543210",
  "totalAmount": 134996.0,
  "currency": "INR",
  "status": "PLACED",
  "deliveryAddress": "Flat 4B, Hitech City, Hyderabad - 500081",
  "estimatedDelivery": "3-5 business days",
  "createdAt": "2026-05-31T22:13:20",
  "items": [
    { "productName": "Samsung Galaxy S24 Ultra", "quantity": 1, "price": 129999.00 },
    { "productName": "Samsung 45W Charger",      "quantity": 1, "price": 2999.00   }
  ]
}
```

---

## Size Optimization History

| Version | Approach | JAR Size |
|---|---|---|
| v1 | AWS SDK v2 + Netty + Log4j | 23 MB |
| v2 | AWS SDK v2 + UrlConnectionClient (no Netty) | 16 MB |
| v3 | AWS SDK v1 + slf4j-simple (no Lombok) | 11 MB |
| **v4 (current)** | **AWS SDK v1 + Lambda Layer (deps separated)** | **~50 KB JAR + 10 MB layer** |

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `Failed to load credentials from IMDS` | `instance-profile: true` set locally | Use `~/.aws/credentials` file instead |
| `Could not find artifact ...log4j2-cachefile-transformer` | Removed from Maven Central | Use `ServicesResourceTransformer` (already fixed) |
| `NoClassDefFoundError` at runtime | Layer not attached | Attach layer in Lambda console |
| Email not received | SES sandbox mode | Verify recipient email in SES console |
| SMS not received | SNS sandbox mode | Verify phone number in SNS console |

---

## Related Repository

| Repository | Description |
|---|---|
| [spring-order-service](https://github.com/voorevamshi/spring-order-service) | Spring Boot REST API that publishes order events to SQS |

---

## License

MIT
