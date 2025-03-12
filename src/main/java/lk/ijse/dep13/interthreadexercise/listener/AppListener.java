package lk.ijse.dep13.interthreadexercise.listener;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import lk.ijse.dep13.interthreadexercise.db.NurseryCP;

import java.util.Set;

public class AppListener implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext) throws ServletException {
        NurseryCP connectionPool = new NurseryCP();
        servletContext.setAttribute("datasource", connectionPool);
    }
}




