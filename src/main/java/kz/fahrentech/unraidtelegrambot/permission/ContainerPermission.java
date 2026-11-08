package kz.fahrentech.unraidtelegrambot.permission;

public record ContainerPermission(
        long id,
        long telegramUserId,
        String containerName,
        boolean canStart,
        boolean canStop,
        boolean canRestart,
        boolean canLogs,
        boolean canExec
) {

    public boolean hasAnyPermission() {
        return canStart || canStop || canRestart || canLogs || canExec;
    }

    public boolean allows(ContainerOperation operation) {
        return switch (operation) {
            case STATUS -> hasAnyPermission();
            case START -> canStart;
            case STOP -> canStop;
            case RESTART -> canRestart;
            case LOGS -> canLogs;
            case EXEC -> canExec;
        };
    }
}
