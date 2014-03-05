package org.ninjatjj;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "Logins", uniqueConstraints = @UniqueConstraint(columnNames = "cookie"))
public class Login implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @NotNull
    @Column(length = 1000)
    private String cookie;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
