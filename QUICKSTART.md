# Quick Start Guide

## Prerequisites

- Docker and Docker Compose installed
- Java 17+ and Maven 3.9+ (optional, for local development)
- Node.js 18+ and npm (optional, for local frontend development)

## Quick Start with Docker

1. **Start all services:**
   ```bash
   docker-compose up --build
   ```

2. **Access the application:**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api

3. **Test the application:**
   - Navigate to http://localhost:3000
   - Click "Simulate Email" to test with sample data
   - View the processed stacks and documents

## Quick Start for Local Development

### Backend Only

1. **Start PostgreSQL:**
   ```bash
   docker run --name invoice-processor-postgres \
     -e POSTGRES_PASSWORD=postgres \
     -e POSTGRES_DB=invoice_processor \
     -p 5432:5432 \
     -d postgres:15-alpine
   ```

2. **Run backend:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

### Frontend Only

1. **Install dependencies:**
   ```bash
   cd frontend
   npm install
   ```

2. **Run frontend:**
   ```bash
   npm start
   ```

   Frontend will be available at http://localhost:4200

## Testing the API

### Simulate an Email

```bash
curl -X POST http://localhost:8080/api/stacks/simulateEmail \
  -H "Content-Type: application/json" \
  -d '{
    "from": "supplier@example.com",
    "to": "invoices@mycompany.com",
    "subject": "Invoice 123",
    "body": "Hello, see attached invoice",
    "attachments": [
      {
        "filename": "invoice123.pdf",
        "type": "PDF_ATTACHMENT",
        "contentReference": "invoice123.pdf"
      }
    ]
  }'
```

### List Stacks

```bash
curl http://localhost:8080/api/stacks
```

### Get Stack Details

```bash
curl http://localhost:8080/api/stacks/{stackId}
```

Replace `{stackId}` with the actual stack ID from the list response.

## Stopping Services

### Docker Compose

```bash
docker-compose down
```

To also remove volumes (database data):

```bash
docker-compose down -v
```

## Troubleshooting

### Port Already in Use

If you get port conflicts, check which ports are in use:

**Windows:**
```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :5432
netstat -ano | findstr :3000
```

**Linux/Mac:**
```bash
lsof -i :8080
lsof -i :5432
lsof -i :3000
```

### Database Connection Issues

If the backend can't connect to PostgreSQL:
1. Check if PostgreSQL container is running: `docker ps`
2. Check logs: `docker logs invoice-processor-postgres`
3. Restart containers: `docker-compose restart`

### Frontend Can't Connect to Backend

1. Ensure backend is running on port 8080
2. Check browser console for CORS errors
3. Verify API URL in `frontend/src/app/services/api.service.ts`

## Next Steps

- Review the [README.md](README.md) for detailed documentation
- Explore the codebase following the hexagonal architecture structure
- Check out the tests in `backend/src/test`
- Customize the LLM stub adapter to integrate with your LLM service
