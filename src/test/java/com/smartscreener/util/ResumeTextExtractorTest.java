package com.smartscreener.util;

import com.smartscreener.exception.ResumeProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumeTextExtractorTest {

    private static final long TEN_MB = 10 * 1024 * 1024;

    private final ResumeTextExtractor extractor =
            new ResumeTextExtractor(TEN_MB);

    @Test
    void extractsTextFromTxtResume() {
        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "candidate.txt",
                "text/plain",
                "Java Spring Boot SQL".getBytes()
        );

        String result = extractor.extractText(file);

        assertThat(result).isEqualTo("Java Spring Boot SQL");
    }

    @Test
    void extractsTextFromPdfResume() throws Exception {
        byte[] pdfBytes = createPdf(
                "Java developer with Spring Boot experience"
        );

        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "candidate.pdf",
                "application/pdf",
                pdfBytes
        );

        String result = extractor.extractText(file);

        assertThat(result)
                .contains("Java developer")
                .contains("Spring Boot");
    }

    @Test
    void rejectsEmptyResume() {
        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertThatThrownBy(() -> extractor.extractText(file))
                .isInstanceOf(ResumeProcessingException.class)
                .hasMessageContaining("non-empty");
    }

    @Test
    void rejectsUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument"
                        + ".wordprocessingml.document",
                "Unsupported document".getBytes()
        );

        assertThatThrownBy(() -> extractor.extractText(file))
                .isInstanceOf(ResumeProcessingException.class)
                .hasMessageContaining("Only PDF and TXT");
    }

    @Test
    void rejectsFakePdfFile() {
        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "fake.pdf",
                "application/pdf",
                "This is not a PDF".getBytes()
        );

        assertThatThrownBy(() -> extractor.extractText(file))
                .isInstanceOf(ResumeProcessingException.class)
                .hasMessageContaining("not a valid PDF");
    }

    @Test
    void rejectsOversizedResume() {
        ResumeTextExtractor smallLimitExtractor =
                new ResumeTextExtractor(5);

        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "large.txt",
                "text/plain",
                "123456".getBytes()
        );

        assertThatThrownBy(
                () -> smallLimitExtractor.extractText(file)
        )
                .isInstanceOf(ResumeProcessingException.class)
                .hasMessageContaining("maximum allowed size");
    }

    @Test
    void sanitizesUploadedFileName() {
        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "../../candidate resume.txt",
                "text/plain",
                "Resume content".getBytes()
        );

        String safeFileName = extractor.getSafeFileName(file);

        assertThat(safeFileName)
                .isEqualTo("candidate_resume.txt");
    }

    private byte[] createPdf(String text) throws Exception {
        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (
                    PDPageContentStream contentStream =
                            new PDPageContentStream(document, page)
            ) {
                contentStream.beginText();
                contentStream.setFont(
                        new PDType1Font(
                                Standard14Fonts.FontName.HELVETICA
                        ),
                        12
                );
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(output);
            return output.toByteArray();
        }
    }
}