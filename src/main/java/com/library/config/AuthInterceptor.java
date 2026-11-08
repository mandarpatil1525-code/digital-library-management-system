package com.library.config;

import com.library.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        User u = (User) req.getSession().getAttribute("user");
        String path = req.getRequestURI();
        if (u == null) { res.sendRedirect("/login"); return false; }
        if (path.startsWith("/admin") && !"ADMIN".equals(u.getRole())) { res.sendRedirect("/user/catalog"); return false; }
        if (path.startsWith("/user") && !"USER".equals(u.getRole())) { res.sendRedirect("/admin/dashboard"); return false; }
        return true;
    }
}
