//package com.datvexpress.ws.versatune.Spring;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.factory.PasswordEncoderFactories;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.provisioning.InMemoryUserDetailsManager;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//@EnableWebSecurity
//public class BasicSecurityConfiguration {
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
////        http
////                .authorizeHttpRequests((requests) -> requests
//// //                       .requestMatchers("/", "/h2-console/**").permitAll()
////                        .anyRequest().authenticated()
////                )
//
////                .formLogin((form) -> form
////                        .loginPage("/login")
////                        .permitAll()
////                )
////                .logout((logout) -> logout.permitAll());
//
//  //      return http.build();
//
//            http
//                .securityMatcher("/","h2-console/**","/edit/**","checkMe","/setup","delete/**","/v1/**","/v2/**","/v3/**")
//                .authorizeHttpRequests( (auth) -> auth
//                        .requestMatchers("/h2-console/**","/").permitAll()
//                        .requestMatchers("/","/edit/**", "/delete/**","/checkMe","/setup","v1/**", "v2/**","v3/**","v4/**").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .httpBasic()
//                .and()
//                .csrf().disable();
//
//        return http.build();
//    }
//
////    @Bean
////    public UserDetailsService userDetailsService() {
////        UserDetails user =
////                User.withDefaultPasswordEncoder()
////                        .username("user")
////                        .password("password")
////                        .roles("USER")
////                        .build();
////
////        return new InMemoryUserDetailsManager(user);
////    }
//
//    @Bean
//    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
//        UserDetails user = User.withUsername("bobt")
//                .password(passwordEncoder.encode("6499n8nt"))
//                .roles("USER")
//                .build();
//
//        UserDetails admin = User.withUsername("admin")
//                .password(passwordEncoder.encode("admin"))
//                .roles("USER", "ADMIN")
//                .build();
//
//        return new InMemoryUserDetailsManager(user, admin);
//    }
//
//     @Bean
//     public PasswordEncoder passwordEncoder() {
//       PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
//       return encoder;
//    }
//}
//
////////////////////////////////// NOT WORKING BELOW ///////////////////////////////////////////////
////package com.datvexpress.ws.versatune.Spring;
////
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////import org.springframework.security.config.annotation.web.builders.HttpSecurity;
////import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
////import org.springframework.security.config.http.SessionCreationPolicy;
////import org.springframework.security.core.userdetails.User;
////import org.springframework.security.core.userdetails.UserDetails;
////import org.springframework.security.crypto.factory.PasswordEncoderFactories;
////import org.springframework.security.crypto.password.PasswordEncoder;
////import org.springframework.security.provisioning.InMemoryUserDetailsManager;
////import org.springframework.security.web.SecurityFilterChain;
////
////@Configuration
////@EnableWebSecurity
////public class BasicSecurityConfiguration {
////
////    @Bean
////    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
////        UserDetails user = User.withUsername("user")
////                .password(passwordEncoder.encode("password"))
////                .roles("USER")
////                .build();
////
////        UserDetails admin = User.withUsername("admin")
////                .password(passwordEncoder.encode("admin"))
////                .roles("USER", "ADMIN")
////                .build();
////
////        return new InMemoryUserDetailsManager(user, admin);
////    }
////
////    @Bean
////    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
////        /*
////        http.authorizeRequests()
////                .anyRequest()
////                .authenticated()
////                .and()
////                .httpBasic();
////        return http.build();
////        */
////
////        /*
////         *  This worked with Spring Boot 2.7.2
////                http.requiresChannel(channel ->
////                channel.anyRequest().requiresSecure())
////                .authorizeRequests()
////                .antMatchers("/lp**","/webjars/**")
////                .anonymous()
////                .antMatchers("/","/edit/**", "/delete/**","/checkMe","/setup","v1/**", "v2/**","v3/**","v4/**","/h2-console/**")
////                .permitAll()
////                .and()
////                .headers().frameOptions().disable()
////                .and()
////                .csrf().disable()
////                .httpBasic();
////        return http.build();
////        */
////        /*  This is for Spring Boot 3.0.0  */
////        http
////                .securityMatcher("/webjars/**",
////                        "/runSlideShow/**",
////                        "/runTuner/**",
////                        "/activeScan",
////                        "/runScan",
////                        "/",
////                        "/edit/**",
////                        "/delete/**",
////                        "/checkMe",
////                        "/setup",
////                        "v1/**",
////                        "v2/**",
////                        "v3/**",
////                        "/h2-console/**")
////                .authorizeHttpRequests( (authrz) -> authrz
////                        .requestMatchers( "/webjars/**",
////                                "/",
////                                "/checkMe",
////                                "/setup",
////                                "v1/**",
////                                "v2/**",
////                                "v3/**",
////                                "/h2-console/**").permitAll()
////                        .requestMatchers("/runSlideShow/**",
////                                "/runTuner/**",
////                                "/activeScan",
////                                "/runScan",
////                                "/edit/**",
////                                "/delete/**").permitAll()
////                        .anyRequest().authenticated()
////                )
////                .httpBasic()
////                .and()
////                .sessionManagement()
////                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
////                .and()
////                .csrf().disable();
////        return http.build();
////
////        /*
////                .authorizeRequests()
////                .antMatchers("/lp**","/webjars/**")
////                .anonymous()
////                .antMatchers("/checkMe","v1/**", "v2/**","v3/**","v4/**","/h2-console/**")
////                .permitAll()
////                .and()
////                .csrf().disable()
////                .httpBasic();
////
////                    http.csrf().disable();
////    http.headers().frameOptions().disable();
////
////
////         */
////    }
////
////    @Bean
////    public PasswordEncoder passwordEncoder() {
////        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
////        return encoder;
////    }
////}
