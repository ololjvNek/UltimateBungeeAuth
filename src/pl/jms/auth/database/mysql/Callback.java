package pl.jms.auth.database.mysql;

public interface Callback<T>
{
    T done(T p0);

    void error(Throwable p0);
}

