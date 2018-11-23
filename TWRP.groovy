int cleanUP() {
    echo "Cleaning"
    dir(env.WORKSPACE) {
            return sh (returnStatus: true, script: '''#!/usr/bin/env bash
            if [ -d "venv" ]; then
                source venv/bin/activate
            else
                virtualenv2 venv
            fi
            make clean && make clobber
        ''')
    }
}

int build() {
    dir(env.WORKSPACE) {
        cleanUP()
        return sh (returnStatus: true, script: '''#!/usr/bin/env bash
        export LC_ALL=C

        if [ -d "venv" ]; then
            source venv/bin/activate
        else
            virtualenv2 venv
        fi

        . build/envsetup.sh
        breakfast omni_"$Device"-eng
        mka recoveryimage
        if [ ! "0" -ne "$?" ]: then
            cd $RESULT_DIR 
            mv recovery.img $Device-recovery.img
        fi
        ''')
    }
}

node('Builder') {
    env
    currentBuild.description = env.Device
    env.WORKSPACE = '/home/jenkins/workspace/TWRP'
    env.RESULT_DIR = env.WORKSPACE + '/out/target/product/' + env.Device
    // env.CCACHE_DIR = env.WORKSPACE + '/.ccache/'
    // env.USE_CCACHE = 1
    // env.CCACHE_NLEVELS = 4
    
    if (env.Sync == 'true') {
        stage('Syncing') {
        
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '/home/jenkins/workspace/TWRP/Build-Tools']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '01481822-0d30-47db-b97a-9990399ced23', url: 'https://github.com/Sudokamikaze/Jenkins-TWRP.git']]]

        checkout poll: false, scm: [$class: 'RepoScm', currentBranch: true, depth: 1, destinationDir: '/home/jenkins/workspace/TWRP', forceSync: true, jobs: 5, localManifest: '''<?xml version="1.0" encoding="UTF-8"?>
        <manifest>

        <!-- REMOVE -->
        <remove-project name="android_bootable_recovery" />

        <!-- MISC -->
        <project path="packages/apps/Nfc" name="platform/packages/apps/Nfc" remote="aosp" />
        <project path="bootable/recovery" name="android_bootable_recovery" remote="omnirom" revision="android-8.1" groups="pdk-cw-fs"/> 

        <!-- TOOLCHAINS -->
        <project path="prebuilts/gcc/linux-x86/arm/arm-eabi-4.6" name="platform/prebuilts/gcc/linux-x86/arm/arm-eabi-4.6" remote="aosp" revision="master" />
        <project path="prebuilts/gcc/linux-x86/arm/arm-eabi-4.8" name="platform/prebuilts/gcc/linux-x86/arm/arm-eabi-4.8" remote="aosp" />
        <project path="prebuilts/gcc/linux-x86/arm/arm-eabi-4.7" name="platform/prebuilts/gcc/linux-x86/arm/arm-eabi-4.7" remote="aosp" revision="master" groups="pdk,linux,arm" />

        <!-- HARDWARE -->
        <project path="hardware/nvidia/tegra3" name="Unlegacy-Android/android_hardware_nvidia_tegra3" revision="stable" remote="github" />
        <project path="hardware/qcom/msm8960" name="platform/hardware/qcom/msm8960" groups="qcom_msm8960" />

        <!-- DEVICES -->
        <project name="clamor95/android_device_unlegacy_recovery" path="device/lenovo/a2109" remote="github" revision="cl2n" />
        <project name="clamor95/android_device_unlegacy_recovery" path="device/asus/flo" remote="github" revision="flo" />
        <project name="clamor95/android_device_unlegacy_recovery" path="device/asus/deb" remote="github" revision="deb" />
        <project name="clamor95/android_device_unlegacy_recovery" path="device/lge/hammerhead" remote="github" revision="hammerhead" />
        <project name="clamor95/android_device_unlegacy_recovery" path="device/lge/mako" remote="github" revision="mako-r" />
        <project name="clamor95/android_device_unlegacy_recovery" path="device/asus/transformer" remote="github" revision="transformer" />

        <!-- KERNELS -->
        <project path="kernel/qcom/msm8960"   name="Unlegacy-Android/android_kernel_qcom_msm8960" remote="github" revision="stable" />
        <project path="kernel/lge/hammerhead" name="Unlegacy-Android/android_kernel_lge_hammerhead" remote="github" revision="stable" />
        <project path="kernel/nvidia/tegra3"  name="clamor95/android_kernel_nvidia_tegra3" remote="github" revision="recovery" />

        <!-- VENDORS -->
        <project path="vendor/nvidia" name="Unlegacy-Android/proprietary_vendor_nvidia" remote="github" revision="aosp-7.1" />
        <project path="vendor/asus" name="Unlegacy-Android/proprietary_vendor_asus" remote="github" revision="aosp-7.1" />
        <project path="vendor/broadcom" name="Unlegacy-Android/proprietary_vendor_broadcom" remote="github" revision="aosp-7.1" />
        <project path="vendor/qcom" name="Unlegacy-Android/proprietary_vendor_qcom" remote="github" revision="aosp-7.1" />
        <project path="vendor/invensense" name="Unlegacy-Android/proprietary_vendor_invensense" remote="github" revision="aosp-7.1" />
        <project path="vendor/lge" name="Unlegacy-Android/proprietary_vendor_lge" remote="github" revision="aosp-7.1" />
        </manifest>''', manifestBranch: 'twrp-7.1', manifestRepositoryUrl: 'git://github.com/minimal-manifest-twrp/platform_manifest_twrp_omni.git']
        }
    }

    stage('Cleanning') {
        // TO DO: Make a check which will define if out directory is dirty or not
        cleanUP()
    }

    stage('Building') {
       ret = build()
       if ( ret != 0 ) {
       cleanUP()
       error('Build failed!')
       } 
    }

    stage('Archiving') {
        dir(env.RESULT_DIR) {
            archiveArtifacts allowEmptyArchive: true, artifacts: '*recovery.img', excludes: 'ramdisk-recovery.*', fingerprint: true, onlyIfSuccessful: true
        }
    }

    stage('Cleanning') {
        cleanUP()
    }
}