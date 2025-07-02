package g03.discord.rbod.paginator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static g03.discord.rbod.RBODMeta.systemMessagePrefix;

public class PaginatorManager implements Paginator {
    private static final long CLEANUP_INTERVAL_SEC = 600;   // 10 minutes
    private static final long SESSION_TIMEOUT_SEC = 1200;   // 20 minutes
    private static final int MAX_PAGE_LENGTH = 1900;        // slightly under Discord's 2000 char limit for extra info in messages

    protected final Map<String, PaginatorSession> sessions = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public PaginatorManager() {
        cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupSessions,  // function to execute
                1800,                   // delay before first execution (30 minutes)
                CLEANUP_INTERVAL_SEC,   // delay between later executions
                TimeUnit.SECONDS
        );
        System.out.println(systemMessagePrefix + "PaginatorManager started:");
        System.out.println("Cleanup interval: " + CLEANUP_INTERVAL_SEC + " seconds.");
        System.out.println("Session timeout: " + SESSION_TIMEOUT_SEC + " seconds.");
        System.out.println("Max page length: " + MAX_PAGE_LENGTH + " characters.");
    }

    @Override
    public PaginatorSession createSession(String ID, List<String> content) {
        List<String> pages = paginate(content);
        PaginatorSession session = new PaginatorSession(pages);
        sessions.put(ID, session);
        System.out.printf(systemMessagePrefix + "Created new phrases session for server %s.\n", ID);
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
        // Update only if there's an active session already
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

    protected List<String> paginate(@NotNull List<String> content) {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            String formattedLine = String.format("%d. %s\n", i+1, content.get(i));
            if (page.length() + formattedLine.length() > MAX_PAGE_LENGTH) {
                String formattedPage = String.format("```Here are the current custom phrases for this server: (Page %d)\n\n%s```", pages.size()+1, page);
                pages.add(formattedPage);
                page = new StringBuilder();
            }
            page.append(formattedLine);
        }
        if (!page.isEmpty()) {
            String formattedPage = String.format("```Here are the current custom phrases for this server: (Page %d)\n\n%s```", pages.size()+1, page);
            pages.add(formattedPage);
        }
        return pages;
    }

    // this will run at a rate defined by the cleanup interval static var
    protected void cleanupSessions() {
        long currentTime = System.currentTimeMillis() / 1000;
        sessions.entrySet().removeIf(
        entry -> currentTime - entry.getValue().getLastAccessed() > SESSION_TIMEOUT_SEC
        );
        System.out.printf(systemMessagePrefix + "Active phrases pagination sessions: %d\n", sessions.size());
    }

    public void shutdown() {
        System.out.println(systemMessagePrefix + "Shutting down PaginatorManager...");
        // shut down the thread to avoid bot getting stuck on shutdown
        cleanupExecutor.shutdown();
    }

}
