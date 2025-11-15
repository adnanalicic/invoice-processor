package com.invoiceprocessor.domain.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

class StackTest {

    @Test
    void testUpdateStatusFromDocuments_AllProcessed_ShouldBeProcessed() {
        Stack stack = new Stack("from@test.com", "to@test.com", "Test Subject");
        stack.setStatus(StackStatus.RECEIVED);

        Document doc1 = new Document();
        doc1.setExtractionStatus(ExtractionStatus.PROCESSED);
        stack.addDocument(doc1);

        Document doc2 = new Document();
        doc2.setExtractionStatus(ExtractionStatus.NOT_APPLICABLE);
        stack.addDocument(doc2);

        stack.updateStatusFromDocuments();

        assertEquals(StackStatus.PROCESSED, stack.getStatus());
    }

    @Test
    void testUpdateStatusFromDocuments_OneError_ShouldBeError() {
        Stack stack = new Stack("from@test.com", "to@test.com", "Test Subject");
        stack.setStatus(StackStatus.RECEIVED);

        Document doc1 = new Document();
        doc1.setExtractionStatus(ExtractionStatus.PROCESSED);
        stack.addDocument(doc1);

        Document doc2 = new Document();
        doc2.setExtractionStatus(ExtractionStatus.ERROR);
        stack.addDocument(doc2);

        stack.updateStatusFromDocuments();

        assertEquals(StackStatus.ERROR, stack.getStatus());
    }

    @Test
    void testUpdateStatusFromDocuments_OneExtracting_ShouldBeProcessing() {
        Stack stack = new Stack("from@test.com", "to@test.com", "Test Subject");
        stack.setStatus(StackStatus.RECEIVED);

        Document doc1 = new Document();
        doc1.setExtractionStatus(ExtractionStatus.EXTRACTING);
        stack.addDocument(doc1);

        stack.updateStatusFromDocuments();

        assertEquals(StackStatus.PROCESSING, stack.getStatus());
    }
}
