package com.example.userservice.service;

import com.example.dto.UserCreatedEvent;
import com.example.userservice.entity.Address;
import com.example.userservice.entity.User;
import com.example.userservice.repository.AddressRepository;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_EVENTS_TOPIC = "user-events";

    @Transactional
    public User createUser(User user) {
        user.setUserId(UUID.randomUUID().toString());
        User savedUser = userRepository.save(user);
        log.info("User created in database: {}", savedUser.getUserId());

        // Publish event to Kafka
        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getUserId(),
                savedUser.getFullName(),
                savedUser.getEmail()
        );
        
        try {
            kafkaTemplate.send(USER_EVENTS_TOPIC, savedUser.getUserId(), event);
            log.info("Successfully published UserCreatedEvent to topic {}: {}", USER_EVENTS_TOPIC, event);
        } catch (Exception e) {
            log.error("Failed to publish UserCreatedEvent to Kafka: {}", e.getMessage(), e);
        }

        return savedUser;
    }

    @Transactional
    public Address addAddress(String userId, Address address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        address.setAddressId(UUID.randomUUID().toString());
        address.setUser(user);
        
        Address savedAddress = addressRepository.save(address);
        log.info("Address {} added for User {}", savedAddress.getAddressId(), userId);
        return savedAddress;
    }

    public User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
