package com.example.miniprojectdelivery.service;

import com.example.miniprojectdelivery.dto.menu.MenuResponseDto;
import com.example.miniprojectdelivery.dto.menu.MenuCreateRequestDto;
import com.example.miniprojectdelivery.dto.menu.MenuUpdateRequestDto;
import com.example.miniprojectdelivery.dto.MessageResponseDto;
import com.example.miniprojectdelivery.model.Menu;
import com.example.miniprojectdelivery.model.Restaurant;
import com.example.miniprojectdelivery.repository.MenuRepository;
import com.example.miniprojectdelivery.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final RestaurantRepository restaurantRepository;
    private final S3Uploader s3Uploader;

    public MenuResponseDto createMenu(MultipartFile image, Long restaurantId, String name, int cost) throws IOException {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(
                () -> new IllegalArgumentException("음식점을 찾을 수 없습니다"));

        // 이미지 S3에 업로드 및 URL 가져오기
        String storedFileName = s3Uploader.upload(image, "images");
        URL imageUrl = new URL(storedFileName);

        Menu menu = menuRepository.save(new Menu(name, cost, restaurant, imageUrl));
        return new MenuResponseDto(menu);
    }


    public List<MenuResponseDto> getMenus() {
        List<Menu> menuList = menuRepository.findAll();
        return menuList.stream().map(MenuResponseDto::new).toList();
    }

    public MenuResponseDto selectMenu(Long id) {
        Menu menu = findMenu(id);
        return new MenuResponseDto(menu);
    }

    @Transactional
    public MenuResponseDto update(Long id, MultipartFile newImage, String name, int cost) throws IOException {
        Menu menu = findMenu(id);
        String oldFileUrl = menu.getImage().getPath().substring(1);
        String updatedImageUrl = s3Uploader.updateFile(newImage, oldFileUrl, "images");
        URL updatedImageUrlObject = new URL(updatedImageUrl);
        menu.update(updatedImageUrlObject, name, cost); // Menu 엔터티 업데이트
        return new MenuResponseDto(menu);
    }
    public MessageResponseDto deleteMenu(Long id) {
        Menu menu = findMenu(id);
        String filePathInS3 = menu.getImage().getPath().substring(1);
        s3Uploader.deleteFile(filePathInS3);
        menuRepository.delete(menu);
        return new MessageResponseDto("메뉴 삭제 성공!");
    }
    // 선택한 메뉴가 존재하는지 검사하는 메서드
    private Menu findMenu(Long id) {
        return menuRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("선택한 메뉴는 존재하지 않습니다."));
    }


}
