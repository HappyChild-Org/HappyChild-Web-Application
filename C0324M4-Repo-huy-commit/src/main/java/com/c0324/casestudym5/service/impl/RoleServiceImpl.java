package com.c0324.casestudym5.service.impl;

import com.c0324.casestudym5.model.Role;
import com.c0324.casestudym5.repository.RoleRepository;
import com.c0324.casestudym5.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role findByName(String name) {
        return roleRepository.findByName(Role.RoleName.valueOf(name));
    }
} 