package g03.discord.rbod.paginator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PaginatorManager {
    private static final long CLEANUP_INTERVAL_SEC = 300; // 5 minutes
    private static final long SESSION_TIMEOUT_SEC = 600; // 10 minutes (possibly increase)
    private static final int MAX_PAGE_LENGTH = 1900; // slightly under Discord's 2000 char limit for extra info

    private final Map<String, PaginatorSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public PaginatorManager() {
        //TODO: Adjust time period
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupSessions,
                CLEANUP_INTERVAL_SEC,
                CLEANUP_INTERVAL_SEC,
                TimeUnit.SECONDS
        );
    }

    public PaginatorSession createSession(String serverId, String channelId, List<String> content) {
        List<String> pages = paginate(content);
        PaginatorSession session = new PaginatorSession(pages);
        String sessionID = String.format("%s-%s", serverId, channelId);
        sessions.put(sessionID, session);
        return session;
    }

    public PaginatorSession getSession(String sessionID) {
        PaginatorSession session = sessions.get(sessionID);
        if (session != null) {
            session.setLastAccessed();
        }
        return session;
    }

    private List<String> paginate(List<String> content) {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        for (int i = 0; i < content.size(); i++ ) {
            String formattedLine = String.format("%d. %s\n", i+1, content.get(i));
            if (page.length() + formattedLine.length() > MAX_PAGE_LENGTH) {
                String formattedPage = String.format("```Here are the current custom phrases for this server: (Page %d)\n%s```", pages.size()+1, page);
                pages.add(formattedPage);
                page = new StringBuilder();
            }
            page.append(formattedLine);
        }
        if (!page.isEmpty()) {
            String formattedPage = String.format("```Here are the current custom phrases for this server: (Page %d)\n%s```", pages.size()+1, page);
            pages.add(formattedPage);
        }
        return pages;
    }

    private void cleanupSessions() {
        long currentTime = System.currentTimeMillis() / 1000;
        sessions.entrySet().removeIf(
                entry -> currentTime - entry.getValue().getLastAccessed() > SESSION_TIMEOUT_SEC
        );
    }

    public void shutdown() { cleanupExecutor.shutdown(); }

}
