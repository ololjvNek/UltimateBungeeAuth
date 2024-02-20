package pl.jms.auth.database.mysql;

public interface Entry
{
    void insert();

    void update(boolean p0);

    void delete();
}
