package g03.discord.rbod.paginator;

import java.util.List;

public class PaginatorSession {
    private List<String> pages;
    private int currentPage;
    private long lastAccessed;

    public PaginatorSession(List<String> pages) {
        this.pages = pages;
        currentPage = 0;
        lastAccessed = System.currentTimeMillis() / 1000;   // time in seconds
    }

    public String getCurrentPage() {
        setLastAccessed();
        return pages.get(currentPage);
    }

    public boolean nextPage() {
        if (currentPage >= pages.size() - 1) {
            return false;
        }
        setLastAccessed();
        currentPage++;
        return true;
    }

    public boolean previousPage() {
        if (currentPage <= 0) {
            return false;
        }
        setLastAccessed();
        currentPage--;
        return true;
    }

    public void setPageNumber(int page) {
//        if (page <= 0 || page > pages.size()) {
//            throw new IllegalArgumentException("Page number must be between 0 and " + (pages.size()-1));
//        }
        // Invalid page number is handled before calling this method
        setLastAccessed();
        currentPage = page-1;
    }

    public int getCurrentPageNumber() { return currentPage + 1; }

    public int getPageCount() { return pages.size(); }

    public long getLastAccessed() { return lastAccessed; }

    public void setLastAccessed() { lastAccessed = System.currentTimeMillis() / 1000; }

    public void setPages(List<String> pages) { this.pages = pages; }
}
