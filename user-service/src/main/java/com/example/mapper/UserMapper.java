package com.example.mapper;

import com.example.dto.response.AddressResponse;
import com.example.dto.response.UserResponse;
import com.example.entity.User;
import com.example.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    AddressResponse toAddressResponse(UserAddress address);

    List<AddressResponse> toAddressResponseList(List<UserAddress> addresses);
}