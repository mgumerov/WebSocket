package ru.slicer;

import javax.annotation.Resource;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import org.h2.tools.RunScript;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;

@WebListener
public class ContextListener implements ServletContextListener {
    @Resource
    private DataSource securityDataSource;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            try (final Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().
                    getResourceAsStream("init.sql"))) {
                RunScript.execute(securityDataSource.getConnection(), reader);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
