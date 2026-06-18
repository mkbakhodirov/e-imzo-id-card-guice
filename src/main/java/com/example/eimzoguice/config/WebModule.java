package com.example.eimzoguice.config;

import com.example.eimzoguice.service.MobileEImzoService;
import com.example.eimzoguice.web.IdCardServlet;
import com.google.gson.Gson;
import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;

public class WebModule extends ServletModule {
    @Override
    protected void configureServlets() {
        bind(AppConfig.class).in(Scopes.SINGLETON);
        bind(MobileEImzoService.class).in(Scopes.SINGLETON);
        bind(Gson.class).in(Scopes.SINGLETON);
        bind(IdCardServlet.class).in(Scopes.SINGLETON);

        serve(
                "/demo",
                "/demo/",
                "/demo/eimzoidcard",
                "/demo/eimzoidcard/",
                "/demo/eimzoidcard/user_auth_result",
                "/demo/eimzoidcard/doc_verify_result"
        ).with(IdCardServlet.class);
    }
}
