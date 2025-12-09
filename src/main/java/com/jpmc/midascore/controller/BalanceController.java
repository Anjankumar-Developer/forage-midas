package com.jpmc.midascore.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Balance;

@RestController
@RequestMapping("/balance")
public class BalanceController {
    
    private final DatabaseConduit databaseConduit;
    
    public BalanceController(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
    }
    
    @GetMapping
    public Balance getBalance(@RequestParam long userId) {
        Optional<UserRecord> userOpt = databaseConduit.findUserById(userId);
        if (userOpt.isPresent()) {
            return new Balance(userOpt.get().getBalance());
        }
        return new Balance(0.0f);
    }
}
