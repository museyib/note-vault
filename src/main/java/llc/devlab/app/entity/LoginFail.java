package llc.devlab.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
public class LoginFail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Long timestamp;

    @Column(nullable = false)
    private Integer attempts;

    private boolean locked;

    private Instant lockedUntil;
}
