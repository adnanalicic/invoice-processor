# Invoice Processor

A **hexagonal architecture monolith** application for processing incoming emails as "stacks" of documents and extracting invoice data using an LLM (currently stubbed).

## Architecture

This application follows **hexagonal/onion architecture** principles:

- **Domain Layer**: Core business entities and services (no dependencies on frameworks)
- **Application Layer**: Use cases and ports (application logic)
- **Adapters**: Web controllers, database repositories, LLM adapter (technical concerns)

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.0
- **Frontend**: Angular 17, Angular Material
- **Database**: PostgreSQL 15
- **Build/Packaging**: Docker, Docker Compose
- **Database Migrations**: Flyway

## Project Structure

```
invoice-processor/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/invoiceprocessor/
│   │   │   │   ├── domain/              # Domain layer (entities, services)
│   │   │   │   ├── application/         # Application layer (use cases, ports)
│   │   │   │   ├── adapter/
│   │   │   │   │   ├── in/web/          # REST controllers
│   │   │   │   │   └── out/
│   │   │   │   │       ├── db/          # JPA repositories
│   │   │   │   │       └── llm/         # LLM adapter (stub)
│   │   │   │   └── config/              # Spring configuration
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/        # Flyway migrations
│   │   └── test/                        # Tests
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/              # Angular components
│   │   │   └── services/                # API service
│   │   └── assets/
│   ├── angular.json
│   └── package.json
└── docker-compose.yml                   # Docker Compose configuration
```

## Core Domain Concepts

### Stack
Represents one incoming email with:
- `id`, `receivedAt`, `fromAddress`, `toAddress`, `subject`
- `status`: RECEIVED, PROCESSING, PROCESSED, ERROR
- Contains one or more Documents

### Document
Any piece of content derived from the email:
- `type`: PDF_ATTACHMENT, IMAGE_ATTACHMENT, EMAIL_BODY, OTHER_ATTACHMENT
- `llmClassification`: INVOICE, NOT_INVOICE, UNKNOWN
- `extractionStatus`: NEW, EXTRACTING, PROCESSED, ERROR, NOT_APPLICABLE

### InvoiceExtraction
Extracted invoice data for a document:
- `invoiceNumber`, `invoiceDate`, `supplierName`, `totalAmount`, `currency`
- One-to-one relationship with Document

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 17+ (for local development)
- Node.js 18+ and npm (for local frontend development)
- Maven 3.9+ (for local backend development)

### Running with Docker Compose (Recommended)

1. **Clone and navigate to the project directory:**
   ```bash
   cd invoice-processor
   ```

2. **Start all services:**
   ```bash
   docker-compose up --build
   ```

   This will:
   - Start PostgreSQL database on port 5432
   - Start Spring Boot backend on port 8080
   - Start Angular frontend on port 3000
   - Run database migrations automatically

3. **Access the application:**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api

### Running Locally (Development)

#### Backend

1. **Start PostgreSQL:**
   ```bash
   docker run --name invoice-processor-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=invoice_processor -p 5432:5432 -d postgres:15-alpine
   ```

2. **Run the Spring Boot application:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

   Backend will start on http://localhost:8080

#### Frontend

1. **Install dependencies:**
   ```bash
   cd frontend
   npm install
   ```

2. **Start the development server:**
   ```bash
   npm start
   ```

   Frontend will start on http://localhost:4200

## API Endpoints

### Stacks

- `GET /api/stacks?page=0&size=20` - List all stacks (paginated)
- `GET /api/stacks/{stackId}` - Get stack details with documents
- `POST /api/stacks/simulateEmail` - Simulate a new email (for testing)

### Documents

- `POST /api/documents/{documentId}/reextract` - Retry extraction for a document

### Example: Simulate Email

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

## Frontend Features

- **Stack List**: View all email stacks with status, document counts
- **Stack Detail**: View stack details, documents, and invoice extractions
- **Simulate Email**: Test interface for simulating incoming emails
- **Retry Extraction**: Manual retry for document extraction

## LLM Integration (Stub)

The current LLM adapter (`StubLlmInvoiceExtractor`) is a simple stub:

- **If** filename or contentReference contains "invoice" (case-insensitive) → Classifies as `INVOICE` and returns dummy valid invoice data
- **Otherwise** → Classifies as `NOT_INVOICE`

**To integrate a real LLM:**
Replace the `StubLlmInvoiceExtractor` implementation in `adapter/out/llm/` with your actual LLM integration.

## Database Migrations

Database migrations are managed by Flyway and located in:
```
backend/src/main/resources/db/migration/
```

Migrations run automatically on application startup.

## Testing

### Backend Tests

Run tests with Maven:
```bash
cd backend
mvn test
```

Test coverage includes:
- Domain entity validation tests
- Domain service logic tests
- LLM stub behavior tests

### Frontend Tests

Run tests with npm:
```bash
cd frontend
npm test
```

## Development Notes

### Architecture Constraints

- **Domain layer** must NOT depend on Spring, JPA, or any framework
- **Application layer** orchestrates domain logic and uses ports
- **Adapters** implement ports and handle technical concerns

### Key Design Decisions

1. **Hexagonal Architecture**: Clear separation between domain, application, and infrastructure
2. **Event-Driven Processing**: Documents are processed asynchronously after stack creation
3. **Status Derivation**: Stack status is derived from document statuses
4. **Validation**: Invoice extraction validation happens at domain level
5. **Stub LLM**: Allows end-to-end testing without external dependencies

### Future Enhancements

- Real email integration (IMAP/POP3)
- Real LLM integration (OpenAI, Anthropic, etc.)
- Authentication/Authorization
- More sophisticated error handling
- Document content storage
- Batch processing
- Webhook support for real-time updates

## Troubleshooting

### Database Connection Issues

If the backend can't connect to PostgreSQL:
- Ensure PostgreSQL container is running: `docker ps`
- Check connection settings in `application.yml`
- Verify database credentials

### Port Conflicts

If ports 3000, 5432, or 8080 are in use:
- Change ports in `docker-compose.yml`
- Update frontend API URL in `api.service.ts` if backend port changes

### Frontend Can't Connect to Backend

- Ensure backend is running on port 8080
- Check CORS settings in `WebConfig.java`
- Verify API base URL in `frontend/src/app/services/api.service.ts`

## License

MIT License - feel free to modify and distribute.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests
5. Submit a pull request
