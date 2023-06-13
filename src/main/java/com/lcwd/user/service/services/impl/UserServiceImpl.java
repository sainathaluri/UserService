package com.lcwd.user.service.services.impl;

import com.lcwd.user.service.entities.Hotel;
import com.lcwd.user.service.entities.Rating;
import com.lcwd.user.service.entities.User;
import com.lcwd.user.service.exceptions.ResourceNotFoundException;
import com.lcwd.user.service.external.services.HotelService;
import com.lcwd.user.service.repositories.UserRepository;
import com.lcwd.user.service.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private HotelService hotelService;

    private Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    @Override
    public User saveUser(User user) {
        String randomUserId = UUID.randomUUID().toString();
        user.setUserId(randomUserId);
        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    @Override
    public User getUser(String userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User with id not found on server " + userId)
        );
        //fetech rating of user from rating service
        Rating[] ratingsOfUser = restTemplate.getForObject
                (
                  "http://RATING-SERVICE/ratings/users/" + user.getUserId(), Rating[].class
                );
        List<Rating> ratings = Arrays.stream(ratingsOfUser).toList();
        logger.info("{}",ratingsOfUser);

        //hotel rating
        List<Rating> ratingList = ratings.stream().map(rating -> {
            //api call to hotel service to get the hotel
            //http://localhost:8082/hotels/9e86a883-a9dd-4e7f-9cc9-bf4de4728956
//            ResponseEntity<Hotel> forEntity = restTemplate.getForEntity
//                    (
//                    "http://HOTEL-SERVICE/hotels/" + rating.getHotelId(),Hotel.class
//                    );
            Hotel hotel = hotelService.getHotel(rating.getHotelId());
//            logger.info("response status code: {} ",forEntity.getStatusCode());

            //set hotel to rating
            rating.setHotel(hotel);
            //return rating
            return rating;
        }).collect(Collectors.toList());

        user.setRatings(ratingList);
        return user;
    }
}
