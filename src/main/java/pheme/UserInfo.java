package pheme;
import timedelayqueue.PubSubMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class UserInfo {

    private final String userName;

    private final UUID usersID;

    private final String password;

    public UserInfo(String username, UUID usersID, String Password) {
        this.userName = username;
        this.usersID = usersID;
        this.password = Password;
    }

    public String getUserName() {
        return userName;
    }

    public UUID getUserID() {
        return usersID;
    }

    public String getPassword() {
        return password;
    }

}
