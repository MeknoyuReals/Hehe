--[[
    MEKNOYU HUB V1 - ROBLOX EXECUTOR COMPATIBLE
    Created by Meknoyu Developer Team
    Official Website: https://meknoyu.com
--]]

local Library = loadstring(game:HttpGet("https://raw.githubusercontent.com/xHeptc/Kavo-UI-Library/main/source.lua"))()
local Window = Library.CreateLib("Meknoyu Hub - Roblox", "Midnight")

-- Tabs
local MainTab = Window:NewTab("Main Features")
local PlayerTab = Window:NewTab("Player Settings")
local CreditTab = Window:NewTab("Credits")

-- Main Tab Sections
local MainSection = MainTab:NewSection("Automation")

MainSection:NewButton("Auto Farm (Level)", "Automatically farm levels in your active game", function()
    print("Auto farm activated via Meknoyu Hub!")
    game:GetService("StarterGui"):SetCore("SendNotification", {
        Title = "Meknoyu Hub",
        Text = "Auto Farm Level Aktif!",
        Duration = 5
    })
end)

MainSection:NewToggle("Auto Collect Chests", "Collect all chests in the map automatically", function(state)
    if state then
        print("Auto chest collection started!")
    else
        print("Auto chest collection stopped.")
    end
end)

-- Player Tab Sections
local PlayerSection = PlayerTab:NewSection("Movement & Stats")

PlayerSection:NewSlider("WalkSpeed", "Ubah kecepatan berjalan karakter kamu", 500, 16, function(s)
    game.Players.LocalPlayer.Character.Humanoid.WalkSpeed = s
end)

PlayerSection:NewSlider("JumpPower", "Ubah kekuatan lompatan karakter kamu", 500, 50, function(s)
    game.Players.LocalPlayer.Character.Humanoid.JumpPower = s
end)

-- Credits Tab
local CreditSection = CreditTab:NewSection("Created by Meknoyu")
CreditSection:NewLabel("Developer: Meknoyu Team")
CreditSection:NewLabel("Support Site: meknoyu.com")
CreditSection:NewButton("Copy Discord Link", "Salin link komunitas", function()
    setclipboard("https://discord.gg/meknoyu")
end)
