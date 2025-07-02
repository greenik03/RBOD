package com.github.greenik03.rbod.paginator;

import java.util.List;

// String ID is the guild's ID
public interface Paginator {

    PaginatorSession createSession(String ID, List<String> content);

    PaginatorSession getSession(String ID);

    boolean updateSession(String ID, List<String> newContent);

    void removeSession(String ID);

}
