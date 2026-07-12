--[[
    UNIVERSAL FLY HACK - MEKNOYU EXECUTOR
    Works on almost any game!
--]]

local LP = game:GetService("Players").LocalPlayer
local Mouse = LP:GetMouse()

local flying = false
local speed = 50

local function startFly()
    local T = LP.Character.PrimaryPart
    local CONTROL = {F = 0, B = 0, L = 0, R = 0}
    local lCONTROL = {F = 0, B = 0, L = 0, R = 0}
    
    local BG = Instance.new("BodyGyro", T)
    local BV = Instance.new("BodyVelocity", T)
    
    BG.P = 9e4
    BG.maxTorque = Vector3.new(9e9, 9e9, 9e9)
    BG.cframe = T.CFrame
    
    BV.velocity = Vector3.new(0, 0.1, 0)
    BV.maxForce = Vector3.new(9e9, 9e9, 9e9)
    
    flying = true
    
    task.spawn(function()
        while flying do
            task.wait()
            if LP.Character:FindFirstChildOfClass("Humanoid").Health <= 0 then
                flying = false
                break
            end
            
            BG.cframe = workspace.CurrentCamera.CFrame
            T.Velocity = Vector3.new(0,0,0)
            
            -- Direction mapping
            local direction = Vector3.new(0,0,0)
            if CONTROL.F ~= 0 or CONTROL.B ~= 0 or CONTROL.L ~= 0 or CONTROL.R ~= 0 then
                direction = ((workspace.CurrentCamera.CFrame.LookVector * (CONTROL.F + CONTROL.B)) + ((workspace.CurrentCamera.CFrame.RightVector * (CONTROL.L + CONTROL.R))))
            end
            
            BV.velocity = direction * speed
        end
        BG:Destroy()
        BV:Destroy()
    end)
end

print("Universal Fly Loaded. Type Fly inside your command executor.")
game:GetService("StarterGui"):SetCore("SendNotification", {
    Title = "Meknoyu Fly Hack",
    Text = "Universal Fly successfully loaded!",
    Duration = 3
})
