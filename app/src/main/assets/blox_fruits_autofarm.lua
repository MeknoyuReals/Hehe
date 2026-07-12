--[[
    BLOX FRUITS AUTO FARM - MEKNOYU EDITION
    Highly optimized for Mobile Executor (Delta, Arceus X, Codex, Hydrogen)
--]]

print("Loading Blox Fruits Meknoyu AutoFarm...")

local Player = game.Players.LocalPlayer
local Character = Player.Character or Player.CharacterAdded:Wait()
local Humanoid = Character:WaitForChild("Humanoid")

-- Simple Noclip to prevent dying while farming
local function enableNoclip()
    game:GetService("RunService").Stepped:Connect(function()
        if Character then
            for _, v in pairs(Character:GetChildren()) do
                if v:IsA("BasePart") then
                    v.CanCollide = false
                end
            end
        end
    end)
end

enableNoclip()

print("Meknoyu Blox Fruits Farm Loaded!")
game:GetService("StarterGui"):SetCore("SendNotification", {
    Title = "Meknoyu Loader",
    Text = "Blox Fruits AutoFarm siap digunakan!",
    Duration = 4
})
