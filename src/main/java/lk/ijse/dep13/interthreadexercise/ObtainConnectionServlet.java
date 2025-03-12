package lk.ijse.dep13.interthreadexercise;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.dep13.interthreadexercise.db.NurseryCP;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "obtain-connection-servlet", urlPatterns = "/connections/random")
public class ObtainConnectionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        NurseryCP connectionPool = (NurseryCP) getServletContext().getAttribute("datasource");


        String newSizeParam = req.getParameter("resize");
        if (newSizeParam != null) {
            try {
                int newSize = Integer.parseInt(newSizeParam);
                connectionPool.resizePool(newSize);
            } catch (NumberFormatException e) {
                resp.getWriter().println("<h2>Invalid pool size!</h2>");
                return;
            } catch (Exception e) {
                resp.getWriter().println("<h2>Error resizing pool: " + e.getMessage() + "</h2>");
                return;
            }
        }

        NurseryCP.ConnectionWrapper cWrapper = connectionPool.getConnection();

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.printf("<h1>Id: %s</h1>", cWrapper.id());
        out.printf("<h1>Connection Ref: %s</h1>", cWrapper.connection());
        out.printf("<h2>Current Pool Size: %d</h2>", connectionPool.getPoolSize());
    }
}


