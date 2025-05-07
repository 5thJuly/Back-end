package org.example.technihongo.core.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JWTAthenticationEntryPoint point;

    @Autowired
    private JWTAuthenticationFilter filter;

    @Autowired
    private CorsFilter corsFilter; // Inject the CorsFilter

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests(authorize -> authorize
                        .requestMatchers("/api/v1/payment/callback", "/api/v1/payment/ipn-handler",
                                "/api/*/login", "/*/*/register", "/*/*/logout", "/*/*/google-auth",
                                "/*/*/getUser/*", "/*/user/*/username", "/*/user/searchStudentName",
                                "/*/user/getUserByStudentId/*", "/*/user/*/password", "/*/user/forgot-password",
                                "/*/user/reset-password", "/*/user/resend-verification-email", "/*/user/verify-email",
                                "/*/user/check-token", "/*/dashboard/student/*", "/*/difficulty-level/**",
                                "/*/achievement/all", "/*/course/all", "/*/course/{id:[0-9]+}", "/*/course/all/*",
                                "/*/domain/all", "/*/domain/parentDomain", "/*/domain/childrenDomain",
                                "/*/domain/getDomain/*", "/*/domain/searchDomainName", "/*/domain/getDomainByTag",
                                "/*/domain/*/getChildrenTag", "/*/flashcard/getFlashcard/*", "/*/flashcard/studentFlashcardSet/*",
                                "/*/flashcard/systemFlashcardSet/*", "/*/learning-path/all", "/*/learning-path/{id:[0-9]+}",
                                "/*/learning-path/search/*", "/*/learning-resource/{id:[0-9]+}", "/*/lesson/{id:[0-9]+}",
                                "/*/lesson/*/paginated/*", "/*/lesson-resource/{id:[0-9]+}", "/*/lesson-resource/lesson/*",
                                "/*/meeting/all", "/*/meeting/{id:[0-9]+}", "/*/script/meeting/*", "/*/script/{id:[0-9]+}",
                                "/*/payment-method/all", "/api/v1/payment/**", "/*/course-progress/view/*", "/*/student-course-rating/getRating/*",
                                "/*/student-course-rating/average/*", "/*/student-course-rating/course/**", "/*/student-flashcard-set/getUserFlashcard/*/*",
                                "/*/*/searchTitle", "/*/flashcard-set-progress/all/*", "/*/flashcard-set-progress/set/*",
                                "/*/resource-progress/*/*", "/*/*/send-reminders", "/*/study-plan/course/*",
                                "/*/study-plan/{id:[0-9]+}", "/*/subscription/all", "/*/subscription/detail/*",
                                "/*/system-flashcard-set/getSysFlashcardSet/*", "/*/system-flashcard-set/getAllFlashcardOfSet/*",
                                "/*/course-progress/all/*", "/*/student-flashcard-set/getAllFlashcardOfSet/*").permitAll()

                        .requestMatchers("/*/lesson-progress/*", "/*/favorite/**", "/*/learning-log/view",
                                "/*/achievement/student/**", "/*/flashcard/*/studentCreate", "/*/flashcard-progress/*",
                                "/*/folder-item/**", "/*/student/**", "/*/course-progress/enroll", "/*/course-progress/check-enroll",
                                "/*/course-progress/track", "/*/student-course-rating/createRating", "/*/student-course-rating/update/*",
                                "/*/student-course-rating/student-rating/course/*", "/*/learning-log/track",
                                "/*/student-flashcard-set/create", "/*/student-flashcard-set/update/*",
                                "/*/student-flashcard-set/updateOrder/*", "/*/student-flashcard-set/delete/*",
                                "/*/student-flashcard-set/updateVisibility/*",
                                "/*/*/getStudentFlashcardSet/*", "/*/*/publicFlashcardSet", "/*/student-flashcard-set/all",
                                "/*/*/from-resource", "/*/*/clone/*", "/*/flashcard-set-progress/track",
                                "/*/flashcard-set-progress/complete", "/*/student-folder/**", "/*/student-quiz-attempt/**",
                                "/*/resource-progress/track", "/*/resource-progress/complete", "/*/resource-progress/note",
                                "/*/student-study-plan/**", "/*/subscription/renew", "/*/subscription/current-plan",
                                "/*/subscription/history", "/*/activity-log/student").hasRole("Student")


                        .requestMatchers("/*/course/create", "/*/course/update/*", "/*/course/creator",
                                "/*/course/creator/*", "/*/course/domain/*", "/*/domain/create", "/*/domain/update/*",
                                "/*/domain/delete/*", "/*/*/*/systemCreate", "/*/learning-path/create",
                                "/*/learning-path/update/*", "/*/learning-path/delete/*", "/*/learning-resource/all",
                                "/*/learning-resource/create", "/*/learning-resource/update/*", "/*/learning-resource/update-status/*",
                                "/*/learning-resource/delete/*", "/*/lesson/create", "/*/lesson/update/*", "/*/lesson/delete/*",
                                "/*/*/update-order/*", "/*/*/set-order/*", "/*/lesson-resource/create",
                                "/*/lesson-resource/update/*", "/*/lesson-resource/delete/*", "/*/lesson-resource/study-plan",
                                "/*/meeting/create", "/*/meeting/update/*", "/*/script/create", "/*/script/update/*",
                                "/*/script/delete/*", "/*/option/create", "/*/option/update", "/*/question/all",
                                "/*/question/create", "/*/question/update/*", "/*/question/options/**", "/*/quiz/all",
                                "/*/quiz/create", "/*/quiz/update/*", "/*/quiz/update-status/*", "/api/quiz-question/create",
                                "/*/quiz-question/delete/*", "/*/quiz-question/create-new-question", "/*/study-plan/create",
                                "/*/study-plan/update/*", "/*/study-plan/delete/*", "/*/system-flashcard-set/create",
                                "/*/system-flashcard-set/update/*", "/*/system-flashcard-set/delete/*", "/*/system-flashcard-set/updateOrder/*",
                                "/*/system-flashcard-set/update-visibility/*", "/*/system-flashcard-set/all",
                                "/*/script/update-order/*", "/*/*/update-status/*", "/*/study-plan/update-status/*",
                                "/*/student-course-rating/allRating", "/*/learning-path/creator", "/*/learning-resource/creator",
                                "/*/quiz/creator", "/*/course-progress/statistics/*").hasRole("Content Manager")


                        .requestMatchers("/*/user/content-manager", "/*/*/*/overview", "/*/*/set-violated/*",
                                "/*/violation/all", "/*/violation/handle/*", "/*/violation/summary",
                                "/*/activity-log/user/*", "/*/*/most-popular-plan", "/*/*/revenue-by-plan",
                                "/*/*/revenue-by-period", "/*/subscription/create", "/*/subscription/update/*",
                                "/*/subscription/delete/*", "/*/user/paginated", "/*/user/student/*",
                                "/*/user/content-managers/*", "/*/user/searchContentManagerName",
                                "/*/payment-method/*/update").hasRole("Administrator")


                        .requestMatchers("/*/flashcard/*/update", "/*/flashcard/delete/*", "/*/option/question/*",
                                "/*/option/{id:[0-9]+}", "/*/question/{id:[0-9]+}", "/*/quiz/{id:[0-9]+}",
                                "/*/quiz-question/quiz/*", "/*/quiz-question/{id:[0-9]+}", "/*/*/questions-options/{id:[0-9]+}",
                                "/*/violation/report").hasAnyRole("Student", "Content Manager")


                        .requestMatchers("/*/statistics/view", "/*/student-course-rating/delete/*").hasAnyRole("Student", "Administrator")


                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(point))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}



