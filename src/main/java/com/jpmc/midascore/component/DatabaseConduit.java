package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DatabaseConduit {
    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    public DatabaseConduit(UserRepository userRepository, TransactionRecordRepository transactionRecordRepository) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);
    }

    public Optional<UserRecord> findUserById(long id) {
        return userRepository.findById(id);
    }

    public void saveTransaction(TransactionRecord transactionRecord) {
        transactionRecordRepository.save(transactionRecord);
    }

    public void updateUserBalance(UserRecord user, float newBalance) {
        user.setBalance(newBalance);
        userRepository.save(user);
    }
}

