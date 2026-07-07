using System.Collections.Concurrent;

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

var users = new Dictionary<string, User>
{
    ["intern"] = new User(
        Username: "intern",
        Password: "123456",
        Balance: 1250
    )
};

var accessTokens = new ConcurrentDictionary<string, TokenRecord>();
var refreshTokens = new ConcurrentDictionary<string, TokenRecord>();

app.MapGet("/", () => "MockBank API is running.");

app.MapPost("/login", (LoginRequest request) =>
{
    if (!users.TryGetValue(request.Username, out var user) || user.Password != request.Password)
    {
        return Results.Unauthorized();
    }

    var accessToken = $"access_{Guid.NewGuid()}";
    var refreshToken = $"refresh_{Guid.NewGuid()}";

    accessTokens[accessToken] = new TokenRecord(
        Username: user.Username,
        ExpiresAt: DateTimeOffset.UtcNow.AddMinutes(15)
    );

    refreshTokens[refreshToken] = new TokenRecord(
        Username: user.Username,
        ExpiresAt: DateTimeOffset.UtcNow.AddDays(7)
    );

    return Results.Ok(new LoginResponse(
        AccessToken: accessToken,
        RefreshToken: refreshToken,
        ExpiresIn: 900
    ));
});

app.MapPost("/refresh", (RefreshRequest request) =>
{
    if (!refreshTokens.TryGetValue(request.RefreshToken, out var record))
    {
        return Results.Unauthorized();
    }

    if (record.ExpiresAt < DateTimeOffset.UtcNow)
    {
        refreshTokens.TryRemove(request.RefreshToken, out _);
        return Results.Unauthorized();
    }

    var newAccessToken = $"access_{Guid.NewGuid()}";

    accessTokens[newAccessToken] = new TokenRecord(
        Username: record.Username,
        ExpiresAt: DateTimeOffset.UtcNow.AddMinutes(15)
    );

    return Results.Ok(new RefreshResponse(
        AccessToken: newAccessToken,
        ExpiresIn: 900
    ));
});

app.MapGet("/balance", (HttpRequest request) =>
{
    var authHeader = request.Headers.Authorization.ToString();

    if (string.IsNullOrWhiteSpace(authHeader) || !authHeader.StartsWith("Bearer "))
    {
        return Results.Unauthorized();
    }

    var token = authHeader["Bearer ".Length..];

    if (!accessTokens.TryGetValue(token, out var record))
    {
        return Results.Unauthorized();
    }

    if (record.ExpiresAt < DateTimeOffset.UtcNow)
    {
        accessTokens.TryRemove(token, out _);
        return Results.Unauthorized();
    }

    var user = users[record.Username];

    return Results.Ok(new BalanceResponse(
        Balance: user.Balance,
        Currency: "TRY"
    ));
});

app.MapPost("/logout", (HttpRequest httpRequest, LogoutRequest request) =>
{
    refreshTokens.TryRemove(request.RefreshToken, out _);

    var authHeader = httpRequest.Headers.Authorization.ToString();

    if (!string.IsNullOrWhiteSpace(authHeader) && authHeader.StartsWith("Bearer "))
    {
        var accessToken = authHeader["Bearer ".Length..];
        accessTokens.TryRemove(accessToken, out _);
    }

    return Results.Ok(new
    {
        message = "Logged out"
    });
});

app.Run();

record User(string Username, string Password, decimal Balance);

record TokenRecord(string Username, DateTimeOffset ExpiresAt);

record LoginRequest(string Username, string Password);

record LoginResponse(string AccessToken, string RefreshToken, int ExpiresIn);

record RefreshRequest(string RefreshToken);

record RefreshResponse(string AccessToken, int ExpiresIn);

record LogoutRequest(string RefreshToken);

record BalanceResponse(decimal Balance, string Currency);