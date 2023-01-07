package com.square.Inventory.Management.System.ServiceImpl;

import com.square.Inventory.Management.System.Constant.InventoryConstant;
import com.square.Inventory.Management.System.DTO.UserDTO;
import com.square.Inventory.Management.System.Entity.User;
import com.square.Inventory.Management.System.ExcelHepler.ExcelHelper;
import com.square.Inventory.Management.System.IMSUtils.EmailUtils;
import com.square.Inventory.Management.System.IMSUtils.InventoryUtils;
import com.square.Inventory.Management.System.JWT.CustomUserServiceDetails;
import com.square.Inventory.Management.System.JWT.JWTFilter;
import com.square.Inventory.Management.System.JWT.JWTUtils;
import com.square.Inventory.Management.System.Repository.BudgetRepository;
import com.square.Inventory.Management.System.Repository.UserRepository;
import com.square.Inventory.Management.System.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired(required = true)
    AuthenticationManager authenticationManager;

    @Autowired
    CustomUserServiceDetails customUserServiceDetails;

    @Autowired
    JWTUtils jwtUtils;

    @Autowired
    JWTFilter jwtFilter;

    @Autowired
    EmailUtils emailUtils;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public ResponseEntity<String> createUser(UserDTO user) {
        try {
            User newUser = userRepository.findByEmail(user.getEmail());
            log.info("Inside log in {}", newUser);
            if (Objects.isNull(newUser)) {

                userRepository.save(getUserFromDTO(user));
                emailUtils.sendMail(user.getEmail(), "Account Approved By" + " " + getCurrentUserName(),
                        "Email: " + user.getEmail() + "\n" + "Password " + user.getPassword() + "\n"
                                + "Please Change Your Password As Soon As possible http//:localhost:8080/inventory/user/changePassword"
                                + "\n" + "Thank You!!!" + "\n" + "\n" + "This mail Send from IMS by Square");
                return InventoryUtils.getResponse("User Register Successful", HttpStatus.CREATED);

            } else {
                return InventoryUtils.getResponse("Email or UserID Already Exist", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return InventoryUtils.getResponse(InventoryConstant.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getCurrentUserName() {

        User user = userRepository.findByEmail(jwtFilter.getCurrentUser());
        return user.getFirstName()+"  "+user.getLastName();
    }

    @Override
    public ResponseEntity<String> login(UserDTO userDTO) {
        try {
            User user=userRepository.findByEmail(userDTO.getEmail());
            if(Objects.nonNull(user)) {
                Authentication auth = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(userDTO.getEmail(), userDTO.getPassword()));
                if (auth.isAuthenticated()) {
                    if (customUserServiceDetails.getUserDetails().getStatus().equalsIgnoreCase("true")) {
                        return new ResponseEntity<String>("{\"token\":\"" + jwtUtils.generateToken(customUserServiceDetails.getUserDetails().getEmail(),
                                customUserServiceDetails.getUserDetails().getRole()) + "\"}", HttpStatus.OK);
                    } else {
                        return InventoryUtils.getResponse("Wait for Approve", HttpStatus.NOT_ACCEPTABLE);
                    }
                }
            } else {
                return InventoryUtils.getResponse("No user found by this Email",HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            log.error("{}", ex);
        }
        return InventoryUtils.getResponse(InventoryConstant.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ByteArrayInputStream load() {
        List<User> users = userRepository.findAll();
        ByteArrayInputStream in = ExcelHelper.UserToExcel(users);
        return in;
    }

    @Override
    public ResponseEntity<String> update(UserDTO user, Long userId) {
        try {
            Optional<User> user1 = userRepository.findById(userId);
            if (user1.isPresent()) {
                User user2 = user1.get();
                user2.setFirstName(user.getFirstName());
                user2.setLastName(user.getLastName());
                user2.setContactNumber(user.getContactNumber());
                userRepository.save(user2);
                return new ResponseEntity<>("User Updated", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("User is not present", HttpStatus.OK);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>("Fail to update", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<?> updateUserRole(String role, Long userID) {
        try {
            Optional<User> user1 = userRepository.findById(userID);
            if (user1.isPresent()) {
                User user2 = user1.get();
                user2.setRole(role);
                userRepository.save(user2);
                return new ResponseEntity<>(user2.getFirstName() + " " + user2.getLastName() + "New Role : " + role, HttpStatus.OK);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>("Fail to upload", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<?> updateUserStatus(String status, Long userID) {
        try {
            Optional<User> user = userRepository.findById(userID);
            if (user.isPresent()) {
                User newUser = user.get();
                newUser.setRole(status);
                userRepository.save(newUser);
                return new ResponseEntity<>(newUser.getFirstName() + " " + newUser.getLastName() + "New Role : " + status, HttpStatus.OK);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>("Fail to upDate Status", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> deleteUser(Long userId) {

        Optional<User> optional = userRepository.findById(userId);
        User user = optional.get();

        if (optional.isPresent() && !"admin".equals(user.getRole())) {

            userRepository.deleteById(userId);
            return new ResponseEntity<>("User Deleted ", HttpStatus.OK);

        } else if (user.getRole().equals("admin")) {
            return new ResponseEntity<>("Can not  Delete Admin ", HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>("User Not Find ", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public List<UserDTO> getAllUserByPagination(int page, int size) {
        Pageable paging = PageRequest.of(page, size);
        Page<UserDTO> pageResult = userRepository.getAllUser(paging);
        if (pageResult.hasContent()) {
            return pageResult.getContent();
        } else {
            return new ArrayList<UserDTO>();
        }
    }

    @Override
    public List<UserDTO> getAllUserByPaginationBySort(int page, int size, String sortBy) {
        Pageable paging = PageRequest.of(page, size, Sort.by(sortBy));
        Page<UserDTO> pageResult = userRepository.getAllUser(paging);
        if (pageResult.hasContent()) {
            return pageResult.getContent();
        } else {
            return new ArrayList<UserDTO>();
        }
    }

    private User getUserFromDTO(UserDTO userDTO) {
        User user = new User();
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setContactNumber(userDTO.getContactNumber());
        user.setEmail(userDTO.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole());
        user.setStatus(userDTO.getStatus());
        return user;
    }

    @Override
    public ResponseEntity<?> forgetPassword(String email) {
//        return ResponseEntity.ok("user found");

    }

}