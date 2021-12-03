package com.ruicheng.blog.initializerstart.controller;

import com.ruicheng.blog.initializerstart.domain.Authority;
import com.ruicheng.blog.initializerstart.domain.User;
import com.ruicheng.blog.initializerstart.repository.UserRepository;
import com.ruicheng.blog.initializerstart.service.AuthorityService;
import com.ruicheng.blog.initializerstart.service.UserService;
import com.ruicheng.blog.initializerstart.vo.Response;
import com.ruicheng.blog.initializerstart.util.ConstraintViolationExceptionHandler;

import javax.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户控制器
 * @author ：Ruicheng
 * @date ：Created in 2021/11/15 14:05
 */
@RestController
@RequestMapping("/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")  // 指定角色权限才能操作方法
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 查询所有用户
     *
     * @param model
     * @return
     */
    @GetMapping
    public ModelAndView list(@RequestParam(value="async",required=false) boolean async,
                             @RequestParam(value="pageIndex",required=false,defaultValue="0") int pageIndex,
                             @RequestParam(value="pageSize",required=false,defaultValue="10") int pageSize,
                             @RequestParam(value="name",required=false,defaultValue="") String name,
                             Model model) {

        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        Page<User> page = userService.listUsersByNameLike(name, pageable);
        List<User> list = page.getContent();	// 当前所在页面数据列表

        model.addAttribute("page", page);
        model.addAttribute("userList", list);
        return new ModelAndView(async==true?"users/list :: #mainContainerRepleace":"users/list", "userModel", model);
    }


    /**
     * 查询用户接口
     *
     * @param model
     * @return
     */
    @GetMapping("{id}")
    public ModelAndView view(@PathVariable("id") Long id, Model model) {
        User user = userRepository.getUserbyId(id);
        model.addAttribute("user", user);
        model.addAttribute("title", "用户信息");
        return new ModelAndView("users/view", "userModel", model);
    }

    /**
     * 获取创建表单页面
     *
     * @param model
     * @return
     */
    @GetMapping("/add")
    public ModelAndView createForm(Model model) {
        model.addAttribute("user", new User(null, null, null, null));
        model.addAttribute("title", "创建用户");
        return new ModelAndView("users/form", "userModel", model);
    }


    /**
     * 新建用户
     * @param user
     * @param authorityId
     * @param authorityId
     * @return
     */
    @PostMapping
    public ResponseEntity<Response> create(User user, Long authorityId) {
        List<Authority> authorities = new ArrayList<>();
        authorities.add(authorityService.getAuthorityById(authorityId));
        user.setAuthorities(authorities);

        if(user.getId() == null) {
            user.setEncodePassword(user.getPassword()); // 加密密码
        }else {
            // 判断密码是否做了变更
            User originalUser = userService.getUserById(user.getId());
            String rawPassword = originalUser.getPassword();
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            String encodePasswd = encoder.encode(user.getPassword());
            boolean isMatch = encoder.matches(rawPassword, encodePasswd);
            if (!isMatch) {
                user.setEncodePassword(user.getPassword());
            }else {
                user.setPassword(user.getPassword());
            }
        }

        try {
            userService.saveUser(user);
        }  catch (ConstraintViolationException e)  {
            return ResponseEntity.ok().body(new Response(false, ConstraintViolationExceptionHandler.getMessage(e)));
        }

        return ResponseEntity.ok().body(new Response(true, "处理成功", user));
    }

    /**
     * 删除用户
     * @param id
     * @return
     */
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Response> delete(@PathVariable("id") Long id, Model model) {
        try {
            userService.removeUser(id);
        } catch (Exception e) {
            return  ResponseEntity.ok().body( new Response(false, e.getMessage()));
        }
        return  ResponseEntity.ok().body( new Response(true, "处理成功"));
    }


    /**
     * 提交表单
     *
     * @param user
     * @return
     */
//    @PostMapping
//    public ModelAndView saveOrUpdateUser(User user) {
//        user = userRepository.saveOrUpdateUser(user);
//        return new ModelAndView("redirect:/users");
//    }

    /**
     * 修改用户信息
     * @param id
     * @param model
     * @return
     */
//    @GetMapping("/modify/{id}")
//    public ModelAndView modify(@PathVariable("id") Long id, Model model) {
//        User user = userRepository.getUserbyId(id);
//        model.addAttribute("user", user);
//        model.addAttribute("title", "修改用户");
//        return new ModelAndView("users/form", "userModel", model);
//    }

    /**
     * 删除用户
     */
//    @GetMapping("/delete/{id}")
//    public ModelAndView view(@PathVariable("id") Long id) {
//        userRepository.deleteUser(id);
//        return new ModelAndView("redirect:/users");
//    }

    /**
     * 获取修改用户的界面，及数据
     * @param id, model
     * @return
     */
    @GetMapping(value = "edit/{id}")
    public ModelAndView modifyForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return new ModelAndView("users/edit", "userModel", model);
    }
}