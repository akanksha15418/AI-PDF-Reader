package com.aipdf.chat;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;

public class CreateSamplePdf {

    public static void main(String[] args) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("AI PDF Chat Assistant - System Overview");
                
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(0, -30);
                contentStream.showText("Retrieval-Augmented Generation (RAG) combines search retrieval with generative LLMs.");
                
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("The backend is powered by Java 21, Spring Boot 3, and LangChain4j.");
                
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Vector embeddings are computed locally using AllMiniLmL6V2 and stored in-memory.");
                
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("The AI Model used for question answering is Google Gemini 1.5 Flash.");
                
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("The project author is Antigravity Pair Programmer, created in 2026.");
                
                contentStream.endText();
            }

            File outputFile = new File("d:/projects/AI PDF project/sample.pdf");
            document.save(outputFile);
            System.out.println("Sample PDF generated successfully at: " + outputFile.getAbsolutePath());
        }
    }
}
