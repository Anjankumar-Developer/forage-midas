package com.jpmc.midascore.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;

@Component
public class KafkaTransactionListener {
    static final Logger logger = LoggerFactory.getLogger(KafkaTransactionListener.class);
    private final List<Transaction> receivedTransactions = new ArrayList<>();
    private final DatabaseConduit databaseConduit;
    private final RestTemplate restTemplate;
    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    public KafkaTransactionListener(DatabaseConduit databaseConduit, RestTemplate restTemplate) {
        this.databaseConduit = databaseConduit;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "${general.kafka-topic}")
    public void listen(Transaction transaction) {
        logger.info("Received transaction: {}", transaction);
        receivedTransactions.add(transaction);
        processTransaction(transaction);
    }

    private void processTransaction(Transaction transaction) {
        // Find sender and recipient
        Optional<UserRecord> senderOpt = databaseConduit.findUserById(transaction.getSenderId());
        Optional<UserRecord> recipientOpt = databaseConduit.findUserById(transaction.getRecipientId());

        // Validate transaction
        if (!senderOpt.isPresent() || !recipientOpt.isPresent()) {
            logger.warn("Transaction rejected: Invalid sender or recipient ID. Transaction: {}", transaction);
            return;
        }

        UserRecord sender = senderOpt.get();
        UserRecord recipient = recipientOpt.get();

        // Check if sender has sufficient balance
        if (sender.getBalance() < transaction.getAmount()) {
            logger.warn("Transaction rejected: Insufficient balance. Transaction: {}", transaction);
            return;
        }

        // Call incentives API to get incentive amount
        float incentiveAmount = 0.0f;
        try {
            Incentive incentive = restTemplate.postForObject(INCENTIVE_API_URL, transaction, Incentive.class);
            if (incentive != null) {
                incentiveAmount = incentive.getAmount();
            }
            logger.info("Incentive received: {}", incentiveAmount);
        } catch (Exception e) {
            logger.warn("Failed to call incentives API: {}", e.getMessage());
            // Continue processing without incentive
        }

        // Process transaction: adjust balances
        float newSenderBalance = sender.getBalance() - transaction.getAmount();
        // Add incentive only to recipient
        float newRecipientBalance = recipient.getBalance() + transaction.getAmount() + incentiveAmount;

        databaseConduit.updateUserBalance(sender, newSenderBalance);
        databaseConduit.updateUserBalance(recipient, newRecipientBalance);

        // Record transaction with incentive
        TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount);
        databaseConduit.saveTransaction(transactionRecord);

        logger.info("Transaction processed successfully: {}", transaction);
    }

    public List<Transaction> getReceivedTransactions() {
        return receivedTransactions;
    }
}

