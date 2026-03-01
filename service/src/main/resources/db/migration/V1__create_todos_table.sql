CREATE TABLE todos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    due_at TIMESTAMP NOT NULL,
    done_at TIMESTAMP NULL,

    CONSTRAINT chk_status
        CHECK (status IN ('NOT_DONE', 'DONE', 'PAST_DUE')),

    CONSTRAINT chk_done_at_consistency
        CHECK (
          (status = 'DONE' AND done_at IS NOT NULL)
          OR
          (status <> 'DONE' AND done_at IS NULL)
        ),

    CONSTRAINT chk_due_after_created
        CHECK (due_at >= created_at)
);

CREATE INDEX idx_todos_status_due_at ON todos(status, due_at);
