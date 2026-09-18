package com.library.model;

/**
 * Enumeration of standardized book categories/genres.
 */
public enum BookCategory {
    COMPUTER_SCIENCE("Computer Science"),
    SOFTWARE_ENGINEERING("Software Engineering"),
    DATA_STRUCTURES("Data Structures & Algorithms"),
    ARTIFICIAL_INTELLIGENCE("Artificial Intelligence"),
    MATHEMATICS("Mathematics"),
    PHYSICS("Physics"),
    ELECTRONICS("Electronics"),
    LITERATURE("Literature"),
    BUSINESS_MANAGEMENT("Business & Management"),
    GENERAL("General Reference");

    private final String displayName;

    BookCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BookCategory fromString(String text) {
        if (text == null) return GENERAL;
        for (BookCategory category : BookCategory.values()) {
            if (category.name().equalsIgnoreCase(text) || category.displayName.equalsIgnoreCase(text)) {
                return category;
            }
        }
        return GENERAL;
    }
}
