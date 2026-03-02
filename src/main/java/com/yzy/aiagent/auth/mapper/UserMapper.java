package com.yzy.aiagent.auth.mapper;

import com.yzy.aiagent.auth.model.UserDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select({
            "SELECT id, username, password_hash, nickname, created_at, updated_at",
            "FROM sys_user",
            "WHERE username = #{username}",
            "LIMIT 1"
    })
    UserDO selectByUsername(String username);

    @Insert({
            "INSERT INTO sys_user(username, password_hash, nickname, created_at, updated_at)",
            "VALUES(#{username}, #{passwordHash}, #{nickname}, #{createdAt}, #{updatedAt})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserDO user);
}
