-- Create stacks table
CREATE TABLE stacks (
    id UUID PRIMARY KEY,
    received_at TIMESTAMP NOT NULL,
    from_address VARCHAR(255) NOT NULL,
    to_address VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL
);

-- Create documents table
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    stack_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    filename VARCHAR(500),
    content_location VARCHAR(1000) NOT NULL,
    llm_classification VARCHAR(50) NOT NULL,
    extraction_status VARCHAR(50) NOT NULL,
    FOREIGN KEY (stack_id) REFERENCES stacks(id) ON DELETE CASCADE
);

-- Create invoice_extractions table
CREATE TABLE invoice_extractions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL UNIQUE,
    invoice_number VARCHAR(255) NOT NULL,
    invoice_date DATE NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_documents_stack_id ON documents(stack_id);
CREATE INDEX idx_documents_extraction_status ON documents(extraction_status);
CREATE INDEX idx_invoice_extractions_document_id ON invoice_extractions(document_id);
CREATE INDEX idx_stacks_received_at ON stacks(received_at DESC);
