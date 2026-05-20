package com.estate.parser.controller;

import com.estate.parser.entity.AdEntity;
import com.estate.parser.repository.AdRepository;
import com.estate.parser.service.loader.LoaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Controller
public class AdController {
    private final AdRepository adRepository;
    private final LoaderService loaderService;

    @Value("${spring.datasource.password}")
    private String parsePassword;

    @GetMapping(path = {"/"})
    public ModelAndView load(@RequestParam(name = "type", defaultValue = "Rental") String type) {
        var types = AdEntity.Type.allBy(type);
        var ads = adRepository.findAllByTypeIn(types, Sort.by(Sort.Direction.DESC, "lastModified"));
        return new ModelAndView("ads", Map.of("ads", ads));
    }

    @RequestMapping(path = "/parse")
    @ResponseBody
    public String triggerParse(@RequestHeader(name = "X-Password", required = false) String password) {
        if (!Objects.equals(password, parsePassword)) {
            return "Wrong password";
        }
        if (loaderService.isRunning()) {
            return "Loader is already running";
        }
        new Thread(loaderService::load, "manual-loader").start();
        return "Loader started";
    }
}
