package com.sparta.blog.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.blog.dto.MessageResponseDto;
import com.sparta.blog.exception.CustomException;
import com.sparta.blog.exception.ErrorCode;
import com.sparta.blog.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j(topic = "JWT 검증 및 인가")
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthorizationFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {

        String tokenValue = jwtUtil.getJwtFromHeader(req);

        if (StringUtils.hasText(tokenValue)) {

            if (!jwtUtil.validateToken(tokenValue)) {
                log.error("Token Error");
                res.setContentType("application/json");
                res.setCharacterEncoding("utf-8");
                MessageResponseDto message = new MessageResponseDto(403, "토큰 님께서 집을 나가셨습니다.");
                res.getWriter().write(new ObjectMapper().writeValueAsString(message));
                return;
            }

            Claims info = jwtUtil.getUserInfoFromToken(tokenValue);

            try {
                setAuthentication(info.getSubject());
            } catch (Exception e) {
                log.error(e.getMessage());
                res.setContentType("application/json");
                res.setCharacterEncoding("utf-8");
                MessageResponseDto message = new MessageResponseDto(403, "토큰 님께서 집을 나가셨습니다.");
                res.getWriter().write(new ObjectMapper().writeValueAsString(message));
                return;
            }
        }

        // 매니저님께 여쭤보기 (필터에서 클라이언트로 반환 어떻게 하는지??)
//        if (tokenValue == null) {
//            log.error("Token Error");
//            res.setContentType("application/json");
//            res.setCharacterEncoding("utf-8");
//            MessageResponseDto message = new MessageResponseDto(403, "토큰 님께서 집을 나가셨습니다.");
//            res.getWriter().write(new ObjectMapper().writeValueAsString(message));
//            return;
//        }

        filterChain.doFilter(req, res);
    }

    // 인증 처리
    public void setAuthentication(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(username);
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
    }

    // 인증 객체 생성
    private Authentication createAuthentication(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
