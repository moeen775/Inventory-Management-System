package com.square.Inventory.Management.System.Service;

import com.square.Inventory.Management.System.Entity.User;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface UserService {
    ResponseEntity<String> signup(Map<String, String> requestMap);

    ResponseEntity<String> login(Map<String, String> requestMap);

    ByteArrayInputStream load();

    ResponseEntity<String> deleteUser(Integer userId);

    List<User> getAllUserByPagination(int page, int size);

    List<User> getAllUserByPaginationBySort(int page, int size, String sortBy);

    List<User> getAllUser();

    ResponseEntity<User> update(User user, Integer userId);
}
