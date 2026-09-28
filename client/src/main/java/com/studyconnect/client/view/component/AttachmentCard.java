package com.studyconnect.client.view.component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Locale;

public class AttachmentCard extends MainTheme.RoundedPanel {

    public AttachmentCard(
            String fileName,
            String details,
            String type
    ) {
        super(14);

        String safeFileName = fileName == null
                ? "Tệp đính kèm"
                : fileName.trim();

        String displayType = resolveDisplayType(
                safeFileName,
                type
        );

        String displayDetails = normalizeDetails(
                details,
                displayType
        );

        setLayout(new BorderLayout(12, 0));
        setFill(new Color(248, 250, 252));
        setBorder(new EmptyBorder(8, 10, 8, 12));

        setPreferredSize(new Dimension(390, 58));
        setMaximumSize(new Dimension(430, 58));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        add(
                createFileIcon(displayType),
                BorderLayout.WEST
        );

        add(
                createFileInformation(
                        safeFileName,
                        displayDetails
                ),
                BorderLayout.CENTER
        );

        setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        setToolTipText(safeFileName);
    }

    private JComponent createFileIcon(String displayType) {
        JLabel fileIcon = new JLabel(
                displayType,
                SwingConstants.CENTER
        );

        fileIcon.setOpaque(true);
        fileIcon.setForeground(Color.WHITE);
        fileIcon.setBackground(
                getFileTypeColor(displayType)
        );

        fileIcon.setFont(
                MainTheme.font(Font.BOLD, 10)
        );

        /*
         * Tăng chiều rộng để DOCX, XLSX, PPTX
         * không bị hiển thị thành "DOC...".
         */
        fileIcon.setPreferredSize(
                new Dimension(50, 40)
        );

        fileIcon.setMinimumSize(
                new Dimension(50, 40)
        );

        return fileIcon;
    }

    private JComponent createFileInformation(
            String fileName,
            String details
    ) {
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);

        textPanel.setLayout(
                new BoxLayout(
                        textPanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel nameLabel = new JLabel(fileName);
        nameLabel.setFont(
                MainTheme.font(Font.BOLD, 13)
        );
        nameLabel.setForeground(MainTheme.NAVY);
        nameLabel.setToolTipText(fileName);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detailsLabel = new JLabel(details);
        detailsLabel.setFont(
                MainTheme.font(Font.PLAIN, 11)
        );
        detailsLabel.setForeground(MainTheme.MUTED);
        detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(detailsLabel);

        return textPanel;
    }

    private String resolveDisplayType(
            String fileName,
            String providedType
    ) {
        String name = fileName == null
                ? ""
                : fileName.toLowerCase(Locale.ROOT);

        String type = providedType == null
                ? ""
                : providedType
                .trim()
                .toLowerCase(Locale.ROOT);

        if (name.endsWith(".pdf")
                || type.equals("application/pdf")
                || type.equals("pdf")) {
            return "PDF";
        }

        if (name.endsWith(".docx")
                || type.contains("wordprocessingml")
                || type.equals("docx")) {
            return "DOCX";
        }

        if (name.endsWith(".doc")
                || type.equals("application/msword")
                || type.equals("doc")) {
            return "DOC";
        }

        if (name.endsWith(".xlsx")
                || type.contains("spreadsheetml")
                || type.equals("xlsx")) {
            return "XLSX";
        }

        if (name.endsWith(".xls")
                || type.equals("application/vnd.ms-excel")
                || type.equals("xls")) {
            return "XLS";
        }

        if (name.endsWith(".pptx")
                || type.contains("presentationml")
                || type.equals("pptx")) {
            return "PPTX";
        }

        if (name.endsWith(".ppt")
                || type.equals("application/vnd.ms-powerpoint")
                || type.equals("ppt")) {
            return "PPT";
        }

        if (name.endsWith(".zip")
                || type.equals("application/zip")) {
            return "ZIP";
        }

        if (name.endsWith(".rar")) {
            return "RAR";
        }

        if (name.endsWith(".txt")
                || type.equals("text/plain")) {
            return "TXT";
        }

        if (name.matches(
                ".*\\.(png|jpg|jpeg|gif|webp|bmp)$"
        ) || type.startsWith("image/")
                || type.equals("image")) {
            return "IMAGE";
        }

        return "FILE";
    }

    private String normalizeDetails(
            String details,
            String displayType
    ) {
        if (details == null || details.isBlank()) {
            return displayType;
        }

        String normalized = details.trim();

        /*
         * Ví dụ:
         * "425.5 KB • DOCUMENT"
         * chuyển thành:
         * "425.5 KB • PDF"
         */
        int separatorIndex = normalized.lastIndexOf('•');

        if (separatorIndex >= 0) {
            return normalized.substring(
                    0,
                    separatorIndex + 1
            ) + " " + displayType;
        }

        if (normalized.equalsIgnoreCase("DOCUMENT")
                || normalized.equalsIgnoreCase("IMAGE")) {
            return displayType;
        }

        return normalized + "  •  " + displayType;
    }

    private Color getFileTypeColor(String type) {
        return switch (type) {
            case "PDF" ->
                    new Color(239, 68, 68);

            case "DOC", "DOCX" ->
                    new Color(37, 121, 230);

            case "XLS", "XLSX" ->
                    new Color(22, 163, 74);

            case "PPT", "PPTX" ->
                    new Color(234, 88, 12);

            case "ZIP", "RAR" ->
                    new Color(126, 87, 194);

            case "IMAGE" ->
                    new Color(5, 150, 105);

            default ->
                    new Color(71, 85, 105);
        };
    }
}