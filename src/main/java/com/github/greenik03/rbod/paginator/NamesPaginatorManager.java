package com.github.greenik03.rbod.paginator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static com.github.greenik03.rbod.RBODMeta.systemMessagePrefix;

public class NamesPaginatorManager implements Paginator {
    private static final long CLEANUP_INTERVAL_SEC = 600;   // 10 minutes
    private static final long SESSION_TIMEOUT_SEC = 1200;   // 20 minutes
    private static final int MAX_PAGE_ITEMS = 20;

    protected final Map<String, PaginatorSession> sessions = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public NamesPaginatorManager() {
        cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupSessions,  // function to execute
                1800,                   // initial delay (30 minutes)
                CLEANUP_INTERVAL_SEC,   // subsequent delay
                TimeUnit.SECONDS
        );
        System.out.println(systemMessagePrefix + "NamesPaginatorManager started:");
        System.out.println("Cleanup interval: " + CLEANUP_INTERVAL_SEC + " seconds.");
        System.out.println("Session timeout: " + SESSION_TIMEOUT_SEC + " seconds.");
        System.out.println("Items per page: " + MAX_PAGE_ITEMS);
    }

    protected List<String> paginate(List<String> content) {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        content.sort(String::compareTo);
        if (!content.isEmpty()) {
            page.append(content.get(0)).append("\n");
        }
        for (int i = 1; i < content.size(); i++) {
            if (i % MAX_PAGE_ITEMS == 0) {
                String formattedPage = String.format("```Here are the current names for the bot in this server: (Page %d)\n\n%s```", pages.size()+1, page);
                pages.add(formattedPage);
                page = new StringBuilder();
            }
            page.append(content.get(i)).append("\n");
        }
        if (!page.isEmpty()) {
            String formattedPage = String.format("```Here are the current names for the bot in this server: (Page %d)\n\n%s```", pages.size()+1, page);
            pages.add(formattedPage);
        }
        return pages;
    }

    protected void cleanupSessions() {
        long currentTime = System.currentTimeMillis() / 1000;
        sessions.entrySet().removeIf(
            entry -> currentTime - entry.getValue().getLastAccessed() > SESSION_TIMEOUT_SEC
        );
        System.out.printf(systemMessagePrefix + "Active name pagination sessions: %d\n", sessions.size());
    }

    public void shutdown() {
        System.out.println(systemMessagePrefix + "Shutting down NamesPaginationManager...");
        cleanupExecutor.shutdown();
    }

    @Override
    public PaginatorSession createSession(String ID, List<String> content) {
        List<String> pages = paginate(content);
        PaginatorSession session = new PaginatorSession(pages);
        sessions.put(ID, session);
        System.out.printf(systemMessagePrefix + "Created new names session for server %s.\n", ID);
        return session;
    }

    @Override
    public PaginatorSession getSession(String ID) {
        PaginatorSession session = sessions.get(ID);
        if (session != null) {
            session.setLastAccessed();
        }
        return session;
    }

    @Override
    public boolean updateSession(String ID, List<String> newContent) {
        if (sessions.containsKey(ID)) {
            newContent = paginate(newContent);
            PaginatorSession updatedSession = sessions.get(ID);
            updatedSession.setPages(newContent);
            sessions.put(ID, updatedSession);
            updatedSession.setLastAccessed();
            return true;
        }
        return false;
    }

    @Override
    public void removeSession(String ID) {
        sessions.remove(ID);
    }
}