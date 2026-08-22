package com.smartscreener.service;

import com.smartscreener.dto.ParsedResumeDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeParserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:\\+?\\d{1,3}[\\s.-]?)?"
                    + "(?:\\(?\\d{3,5}\\)?[\\s.-]?)?"
                    + "\\d{3,5}[\\s.-]?\\d{4}(?!\\d)"
    );

    private static final Pattern POSSIBLE_NAME_PATTERN = Pattern.compile(
            "^[\\p{L}][\\p{L} .'-]{1,78}$"
    );

    private static final Set<String> INVALID_NAME_WORDS = Set.of(
            "resume",
            "curriculum",
            "vitae",
            "profile",
            "email",
            "phone",
            "mobile",
            "address",
            "summary",
            "objective"
    );

    private static final Set<String> EXPERIENCE_HEADINGS = Set.of(
            "experience",
            "work experience",
            "professional experience",
            "employment history",
            "work history",
            "internship",
            "internships"
    );

    private static final Set<String> EDUCATION_HEADINGS = Set.of(
            "education",
            "academic background",
            "academic qualifications",
            "educational qualifications",
            "qualifications"
    );

    private static final Set<String> KNOWN_SECTION_HEADINGS = Set.of(
            "summary",
            "professional summary",
            "career objective",
            "objective",
            "skills",
            "technical skills",
            "experience",
            "work experience",
            "professional experience",
            "employment history",
            "work history",
            "internship",
            "internships",
            "education",
            "academic background",
            "academic qualifications",
            "educational qualifications",
            "qualifications",
            "projects",
            "certifications",
            "achievements",
            "awards",
            "languages",
            "interests",
            "declaration",
            "references"
    );

    private static final Map<String, Pattern> SKILL_PATTERNS =
            createSkillPatterns();

    public ParsedResumeDto parse(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException(
                    "Resume text must not be empty."
            );
        }

        String normalizedText = normalizeLineEndings(resumeText);

        return new ParsedResumeDto(
                extractName(normalizedText),
                extractFirstMatch(EMAIL_PATTERN, normalizedText),
                extractPhone(normalizedText),
                extractSkills(normalizedText),
                extractSection(normalizedText, EXPERIENCE_HEADINGS),
                extractSection(normalizedText, EDUCATION_HEADINGS)
        );
    }

    private String extractName(String text) {
        String[] lines = text.split("\\n");

        int inspectedLines = Math.min(lines.length, 10);

        for (int index = 0; index < inspectedLines; index++) {
            String line = lines[index].trim();

            if (line.isBlank() || line.length() > 80) {
                continue;
            }

            String lowerCaseLine = line.toLowerCase(Locale.ROOT);

            boolean containsInvalidWord = INVALID_NAME_WORDS.stream()
                    .anyMatch(lowerCaseLine::contains);

            if (containsInvalidWord ||
                    EMAIL_PATTERN.matcher(line).find() ||
                    containsDigit(line)) {
                continue;
            }

            if (POSSIBLE_NAME_PATTERN.matcher(line).matches()) {
                return line;
            }
        }

        return "Not identified";
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);

        while (matcher.find()) {
            String phone = matcher.group().trim();
            long digitCount = phone.chars()
                    .filter(Character::isDigit)
                    .count();

            if (digitCount >= 10 && digitCount <= 13) {
                return phone;
            }
        }

        return "Not identified";
    }

    private List<String> extractSkills(String text) {
        List<String> detectedSkills = new ArrayList<>();

        for (Map.Entry<String, Pattern> entry :
                SKILL_PATTERNS.entrySet()) {

            if (entry.getValue().matcher(text).find()) {
                detectedSkills.add(entry.getKey());
            }
        }

        return detectedSkills;
    }

    private String extractSection(
            String text,
            Set<String> targetHeadings
    ) {
        String[] lines = text.split("\\n");
        StringBuilder section = new StringBuilder();
        boolean collecting = false;

        for (String originalLine : lines) {
            String line = originalLine.trim();
            String normalizedLine = normalizeHeading(line);

            if (!collecting &&
                    targetHeadings.contains(normalizedLine)) {
                collecting = true;
                continue;
            }

            if (collecting &&
                    KNOWN_SECTION_HEADINGS.contains(normalizedLine)) {
                break;
            }

            if (collecting && !line.isBlank()) {
                if (section.length() > 0) {
                    section.append(System.lineSeparator());
                }

                section.append(line);

                if (section.length() >= 3000) {
                    break;
                }
            }
        }

        if (section.isEmpty()) {
            return "Not clearly specified";
        }

        return section.toString();
    }

    private String extractFirstMatch(
            Pattern pattern,
            String text
    ) {
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group().trim();
        }

        return "Not identified";
    }

    private boolean containsDigit(String value) {
        return value.chars().anyMatch(Character::isDigit);
    }

    private String normalizeLineEndings(String text) {
        return text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
    }

    private String normalizeHeading(String line) {
        return line
                .toLowerCase(Locale.ROOT)
                .replaceAll("[:\\-]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static Map<String, Pattern> createSkillPatterns() {
        Map<String, Pattern> patterns = new LinkedHashMap<>();

        addSkill(patterns, "Java", "\\bjava\\b");
        addSkill(patterns, "Spring Boot", "\\bspring\\s*boot\\b");
        addSkill(patterns, "Spring MVC", "\\bspring\\s*mvc\\b");
        addSkill(patterns, "Hibernate", "\\bhibernate\\b");
        addSkill(patterns, "JPA", "\\bjpa\\b");
        addSkill(patterns, "Python", "\\bpython\\b");
        addSkill(patterns, "JavaScript", "\\bjavascript\\b");
        addSkill(patterns, "TypeScript", "\\btypescript\\b");
        addSkill(patterns, "React", "\\breact(?:\\.js|js)?\\b");
        addSkill(patterns, "Angular", "\\bangular\\b");
        addSkill(patterns, "Node.js", "\\bnode(?:\\.js|js)\\b");
        addSkill(patterns, "HTML", "\\bhtml5?\\b");
        addSkill(patterns, "CSS", "\\bcss3?\\b");
        addSkill(patterns, "SQL", "\\bsql\\b");
        addSkill(patterns, "MySQL", "\\bmysql\\b");
        addSkill(patterns, "PostgreSQL", "\\bpostgres(?:ql)?\\b");
        addSkill(patterns, "MongoDB", "\\bmongodb\\b");
        addSkill(patterns, "Oracle", "\\boracle\\b");
        addSkill(patterns, "Git", "\\bgit\\b");
        addSkill(patterns, "GitHub", "\\bgithub\\b");
        addSkill(patterns, "Docker", "\\bdocker\\b");
        addSkill(patterns, "Kubernetes", "\\bkubernetes\\b");
        addSkill(patterns, "AWS", "\\baws\\b");
        addSkill(patterns, "Azure", "\\bazure\\b");
        addSkill(patterns, "GCP", "\\bgcp\\b");
        addSkill(patterns, "REST API", "\\brest(?:ful)?\\s*api(?:s)?\\b");
        addSkill(patterns, "Microservices", "\\bmicroservices?\\b");
        addSkill(patterns, "Maven", "\\bmaven\\b");
        addSkill(patterns, "Gradle", "\\bgradle\\b");
        addSkill(patterns, "JUnit", "\\bjunit\\b");
        addSkill(patterns, "Selenium", "\\bselenium\\b");
        addSkill(patterns, "C", "(?<![+#\\w])c(?![+#\\w])");
        addSkill(patterns, "C++", "(?<!\\w)c\\+\\+(?!\\w)");
        addSkill(patterns, "C#", "(?<!\\w)c#(?!\\w)");
        addSkill(patterns, ".NET", "(?<!\\w)\\.net\\b");
        addSkill(patterns, "Machine Learning",
                "\\bmachine\\s+learning\\b");
        addSkill(patterns, "Deep Learning",
                "\\bdeep\\s+learning\\b");
        addSkill(patterns, "TensorFlow", "\\btensorflow\\b");
        addSkill(patterns, "PyTorch", "\\bpytorch\\b");
        addSkill(patterns, "NLP",
                "\\bnlp\\b|\\bnatural\\s+language\\s+processing\\b");

        return Map.copyOf(patterns);
    }

    private static void addSkill(
            Map<String, Pattern> patterns,
            String displayName,
            String regularExpression
    ) {
        patterns.put(
                displayName,
                Pattern.compile(
                        regularExpression,
                        Pattern.CASE_INSENSITIVE
                )
        );
    }
}