document.addEventListener("DOMContentLoaded", () => {

    const container = document.getElementById("container");
    const registerBtn = document.getElementById("register");
    const loginBtn = document.getElementById("login");

    const signupButton = document.getElementById("signupBtn");
    const loginButton = document.getElementById("loginBtn");

    // Animation: Open Registration
    if (registerBtn) {
        registerBtn.addEventListener("click", () => {
            container.classList.add("active");
        });
    }

  
    if (loginBtn) {
        loginBtn.addEventListener("click", () => {
            container.classList.remove("active");
        });
    }

    // Registration Handler
    if (signupButton) {
        signupButton.addEventListener("click", async (e) => {
            e.preventDefault();

            const nameInput = document.getElementById("signupName");
            const emailInput = document.getElementById("signupEmail");
            const passwordInput = document.getElementById("signupPassword");

            const name = nameInput ? nameInput.value.trim() : "";
            const email = emailInput ? emailInput.value.trim() : "";
            const password = passwordInput ? passwordInput.value : "";

            if (!name || !email || !password) {
                alert("Please fill in all fields.");
                return;
            }

            if (password.length < 6) {
                alert("Password must be at least 6 characters long.");
                return;
            }

            try {
                const response = await fetch("/api/register", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        name: name,
                        email: email,
                        password: password
                    })
                });

                const result = await response.json();

                if (result.success) {
                    alert("Registration successful! You can now log in.");

                    if (nameInput) nameInput.value = "";
                    if (emailInput) emailInput.value = "";
                    if (passwordInput) passwordInput.value = "";

                    if (container) container.classList.remove("active");
                } else {
                    alert(result.message || "Registration failed.");
                }

            } catch (error) {
                console.error("Registration Error:", error);
                alert("Cannot connect to server. Make sure the Java server is running.");
            }
        });
    }

    
    // Login Handler
    if (loginButton) {
        loginButton.addEventListener("click", async (e) => {
            e.preventDefault();

            const emailInput = document.getElementById("loginEmail");
            const passwordInput = document.getElementById("loginPassword");

            const email = emailInput ? emailInput.value.trim() : "";
            const password = passwordInput ? passwordInput.value : "";

            if (!email || !password) {
                alert("Please enter both email and password.");
                return;
            }

            try {
                const response = await fetch("/api/login", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        email: email,
                        password: password
                    })
                });

                const result = await response.json();


                if (result.success) {
                    // alert(`Login successful! Welcome back, ${result.user.name}.`);

                    localStorage.setItem("user", JSON.stringify(result.user));

                    window.location.href = "/profile";

                } else {
                    alert(result.message || "Login failed.");
                }
            } catch (error) {
                console.error("Login Error:", error);
                alert("Cannot connect to server. Make sure the Java server is running.");
            }
        });
    }

});