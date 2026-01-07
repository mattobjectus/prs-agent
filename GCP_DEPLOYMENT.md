# Google Cloud Run Deployment Guide

This guide explains how to deploy the PRS Knowledge Agent application to Google Cloud Run.

## 🏗️ Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  Cloud Run      │
│  (Container)    │
└────────┬────────┘
         │
         ├──────────────┐
         │              │
         ▼              ▼
┌─────────────┐  ┌─────────────┐
│  Redis      │  │   Grok AI   │
│  (Memorystore│  │   (xAI)     │
│   or Cloud  │  └─────────────┘
│   Run Redis)│
└─────────────┘
```

## 📋 Prerequisites

1. **Google Cloud Account** with billing enabled
2. **gcloud CLI** installed and configured
3. **Docker** (optional, for local testing)
4. **Redis Instance** (Memorystore or external)

## 🚀 Quick Deploy

### 1. Set Up Google Cloud Project

```bash
# Set your project ID
export PROJECT_ID=your-project-id
export REGION=us-central1

# Set the project
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable \
    cloudbuild.googleapis.com \
    run.googleapis.com \
    containerregistry.googleapis.com \
    secretmanager.googleapis.com
```

### 2. Create Redis Instance

#### Option A: Cloud Memorystore (Recommended for Production)

```bash
# Create Redis instance
gcloud redis instances create prs-redis \
    --size=1 \
    --region=$REGION \
    --redis-version=redis_7_0

# Get the Redis host
export REDIS_HOST=$(gcloud redis instances describe prs-redis \
    --region=$REGION \
    --format="value(host)")

echo "Redis Host: $REDIS_HOST"
```

#### Option B: External Redis

```bash
# Use your existing Redis connection details
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
```

### 3. Store Secrets in Secret Manager

```bash
# Create the AI API Key secret
echo -n "your-actual-xai-api-key" | \
    gcloud secrets create ai-api-key \
    --data-file=-

# Grant Cloud Run access to the secret
gcloud secrets add-iam-policy-binding ai-api-key \
    --member=serviceAccount:$PROJECT_ID@appspot.gserviceaccount.com \
    --role=roles/secretmanager.secretAccessor
```

### 4. Update cloudbuild.yaml

Edit `cloudbuild.yaml` and update the substitutions:

```yaml
substitutions:
    _REDIS_HOST: "your-redis-host-ip"
    _REDIS_PORT: "6379"
```

### 5. Deploy with Cloud Build

#### Option A: From Local Machine

```bash
# Submit build to Cloud Build
gcloud builds submit \
    --config=cloudbuild.yaml \
    --substitutions=_REDIS_HOST=$REDIS_HOST,_REDIS_PORT=6379
```

#### Option B: Continuous Deployment from GitHub

1. **Connect your GitHub repository**:

```bash
# Link GitHub repo (first time only)
gcloud alpha builds triggers create github \
    --repo-name=prs-agent \
    --repo-owner=your-github-username \
    --branch-pattern="^main$" \
    --build-config=cloudbuild.yaml \
    --substitutions=_REDIS_HOST=$REDIS_HOST,_REDIS_PORT=6379
```

2. **Push to GitHub** - deployment happens automatically!

### 6. Get Your Service URL

```bash
gcloud run services describe prs-knowledge-agent \
    --region=$REGION \
    --format="value(status.url)"
```

## 🔧 Manual Deployment (Alternative)

If you prefer manual control:

```bash
# 1. Build the image
docker build -t gcr.io/$PROJECT_ID/prs-knowledge-agent:latest .

# 2. Push to Container Registry
docker push gcr.io/$PROJECT_ID/prs-knowledge-agent:latest

# 3. Deploy to Cloud Run
gcloud run deploy prs-knowledge-agent \
    --image=gcr.io/$PROJECT_ID/prs-knowledge-agent:latest \
    --region=$REGION \
    --platform=managed \
    --allow-unauthenticated \
    --set-env-vars="REDIS_HOST=$REDIS_HOST,REDIS_PORT=6379" \
    --set-secrets="AI_API_KEY=ai-api-key:latest" \
    --memory=2Gi \
    --cpu=2 \
    --timeout=300 \
    --max-instances=10 \
    --min-instances=1
```

## ⚙️ Configuration

### Environment Variables

Set via `--set-env-vars` flag:

| Variable     | Description                         | Example    |
| ------------ | ----------------------------------- | ---------- |
| `REDIS_HOST` | Redis hostname                      | `10.0.0.3` |
| `REDIS_PORT` | Redis port                          | `6379`     |
| `PORT`       | Server port (auto-set by Cloud Run) | `8080`     |

### Secrets

Set via `--set-secrets` flag (from Secret Manager):

| Secret       | Description      | Required |
| ------------ | ---------------- | -------- |
| `AI_API_KEY` | xAI Grok API key | Yes      |

### Resource Configuration

```bash
# Update resources
gcloud run services update prs-knowledge-agent \
    --region=$REGION \
    --memory=4Gi \
    --cpu=4 \
    --timeout=600 \
    --max-instances=20 \
    --min-instances=2
```

### Autoscaling

```bash
# Configure autoscaling
gcloud run services update prs-knowledge-agent \
    --region=$REGION \
    --min-instances=1 \
    --max-instances=10 \
    --concurrency=80
```

## 🔒 Security

### Enable Authentication

```bash
# Require authentication
gcloud run services update prs-knowledge-agent \
    --region=$REGION \
    --no-allow-unauthenticated

# Grant access to specific users
gcloud run services add-iam-policy-binding prs-knowledge-agent \
    --region=$REGION \
    --member=user:alice@example.com \
    --role=roles/run.invoker
```

### VPC Connector (for Memorystore)

```bash
# Create VPC connector
gcloud compute networks vpc-access connectors create prs-connector \
    --region=$REGION \
    --range=10.8.0.0/28

# Update Cloud Run to use VPC
gcloud run services update prs-knowledge-agent \
    --region=$REGION \
    --vpc-connector=prs-connector
```

## 📊 Monitoring

### View Logs

```bash
# Stream logs
gcloud run services logs read prs-knowledge-agent \
    --region=$REGION \
    --limit=50 \
    --follow

# View in Cloud Console
open "https://console.cloud.google.com/logs/query?project=$PROJECT_ID"
```

### Metrics

```bash
# View metrics in Cloud Console
open "https://console.cloud.google.com/run/detail/$REGION/prs-knowledge-agent/metrics?project=$PROJECT_ID"
```

## 🔄 Updates and Rollbacks

### Update Application

```bash
# Option 1: Submit new build
gcloud builds submit --config=cloudbuild.yaml

# Option 2: Deploy new image directly
gcloud run deploy prs-knowledge-agent \
    --region=$REGION \
    --image=gcr.io/$PROJECT_ID/prs-knowledge-agent:$NEW_TAG
```

### Rollback

```bash
# List revisions
gcloud run revisions list \
    --service=prs-knowledge-agent \
    --region=$REGION

# Rollback to previous revision
gcloud run services update-traffic prs-knowledge-agent \
    --region=$REGION \
    --to-revisions=prs-knowledge-agent-00002-xxx=100
```

## 💰 Cost Optimization

### Strategies

1. **Use minimum instances = 0** (cold starts acceptable)

```bash
gcloud run services update prs-knowledge-agent \
    --region=$REGION \
    --min-instances=0
```

2. **Reduce memory** (if possible)

```bash
gcloud run services update prs-knowledge-agent \
    --region=$REGION \
    --memory=1Gi
```

3. **Set request limits**

```bash
gcloud run services update prs-knowledge-agent \
    --region=$REGION \
    --max-instances=5
```

### Estimated Costs

-   **Cloud Run**: $0.00002400/vCPU-second, $0.00000250/GiB-second
-   **Container Registry**: $0.026/GB/month
-   **Memorystore**: Starting at $0.065/GB/hour
-   **Cloud Build**: 120 build-minutes/day free

## 🐛 Troubleshooting

### Application Not Starting

```bash
# Check logs
gcloud run services logs tail prs-knowledge-agent --region=$REGION

# Common issues:
# 1. Redis connection: Check VPC connector or Redis IP
# 2. Secrets not accessible: Check IAM permissions
# 3. Memory limit: Increase --memory
```

### Redis Connection Issues

```bash
# Test Redis connectivity
gcloud compute ssh test-vm --zone=us-central1-a --command="
  redis-cli -h $REDIS_HOST ping
"

# For Memorystore, ensure VPC connector is configured
```

### Secret Not Loading

```bash
# Verify secret exists
gcloud secrets describe ai-api-key

# Check IAM permissions
gcloud secrets get-iam-policy ai-api-key
```

## 🔗 Useful Commands

```bash
# Describe service
gcloud run services describe prs-knowledge-agent --region=$REGION

# Get service URL
gcloud run services describe prs-knowledge-agent \
    --region=$REGION \
    --format="value(status.url)"

# Update single environment variable
gcloud run services update prs-knowledge-agent \
    --region=$REGION \
    --update-env-vars="NEW_VAR=value"

# Delete service
gcloud run services delete prs-knowledge-agent --region=$REGION
```

## 📚 Additional Resources

-   [Cloud Run Documentation](https://cloud.google.com/run/docs)
-   [Cloud Build Documentation](https://cloud.google.com/build/docs)
-   [Secret Manager Documentation](https://cloud.google.com/secret-manager/docs)
-   [Memorystore for Redis](https://cloud.google.com/memorystore/docs/redis)

## ✅ Deployment Checklist

-   [ ] Enable required GCP APIs
-   [ ] Create Redis instance (Memorystore or external)
-   [ ] Store API key in Secret Manager
-   [ ] Update cloudbuild.yaml with Redis details
-   [ ] Submit build with Cloud Build
-   [ ] Configure VPC connector (if using Memorystore)
-   [ ] Test the deployed service
-   [ ] Set up monitoring and alerts
-   [ ] Configure autoscaling
-   [ ] Set up continuous deployment (optional)

---

**🎉 Your PRS Knowledge Agent is now running on Google Cloud Run!**

Access your service at the URL provided by:

```bash
gcloud run services describe prs-knowledge-agent --region=us-central1 --format="value(status.url)"
```
