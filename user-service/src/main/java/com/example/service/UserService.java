package com.example.service;

import com.example.dto.request.AddressRequest;
import com.example.dto.request.UpdateProfileRequest;
import com.example.dto.response.AddressResponse;
import com.example.dto.response.UserResponse;
import com.example.entity.User;
import com.example.entity.UserAddress;
import com.example.exception.NotFoundException;
import com.example.mapper.UserMapper;
import com.example.repository.UserAddressRepository;
import com.example.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class UserService {

    @Inject UserRepository userRepository;
    @Inject UserAddressRepository addressRepository;
    @Inject UserMapper userMapper;

    // ─── Profile ──────────────────────────────────────────────────────────────

    public UserResponse getProfile(String userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest req) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (req.fullName != null) user.setFullName(req.fullName);
        if (req.phone != null) user.setPhone(req.phone);

        return userMapper.toResponse(user);
    }

    @Transactional
    public void deleteAccount(String userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.softDelete();
    }

    // ─── Addresses ────────────────────────────────────────────────────────────

    public List<AddressResponse> getAddresses(String userId) {
        List<UserAddress> addresses = addressRepository.findByUserId(userId);
        return userMapper.toAddressResponseList(addresses);
    }

    @Transactional
    public AddressResponse addAddress(String userId, AddressRequest req) {

        if (Boolean.TRUE.equals(req.isDefault)) {
            addressRepository.clearDefaultForUser(userId);
        }

        UserAddress address = UserAddress.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .receiverName(req.receiverName)
                .phone(req.phone)
                .addressLine(req.addressLine)
                .ward(req.ward)
                .district(req.district)
                .city(req.city)
                .isDefault(Boolean.TRUE.equals(req.isDefault))
                .build();

        addressRepository.persist(address);
        addressRepository.flush();
        return userMapper.toAddressResponse(address);
    }

    public Response getContact(String userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Map<String, Object> contact = new java.util.LinkedHashMap<>();
        contact.put("id", user.getId());
        contact.put("email", user.getEmail());
        contact.put("phone", user.getPhone());
        contact.put("fullName", user.getFullName());
        return Response.ok(contact).build();
    }

    @Transactional
    public void deleteAddress(String userId, String addressId) {
        System.out.println("DEBUG delete - addressId: " + addressId + ", userId: " + userId);

        addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address not found"));

        long deleted = addressRepository.delete("id = ?1 and userId = ?2", addressId, userId);
        System.out.println("DEBUG deleted rows: " + deleted);
    }
}