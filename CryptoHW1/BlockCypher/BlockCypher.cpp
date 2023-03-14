//
//  BlockCypher.cpp
//  BlockCypher
//
//  Created by Avishek Choudhury on 3/8/23.
//

#include <array>
#include <string>
#include <iostream>
#include <algorithm>
#include <random>
#include <chrono>

//std::array<uint8_t, 256> generateSubstitution(std::array<uint8_t, 256> inptArr) {
//    std::array<uint8_t, 256> newArr;
//    for (int i = 0; i < 256; i++) {
//        int randInt = i + ((rand() % 256) - i);
//        newArr[randInt] = inptArr[i];
//        newArr[i] = inptArr[randInt];
//    }
//    return newArr;
//}

std::array<uint8_t, 256> subst0;
std::array<uint8_t, 256> subst1;
std::array<uint8_t, 256> subst2;
std::array<uint8_t, 256> subst3;
std::array<uint8_t, 256> subst4;
std::array<uint8_t, 256> subst5;
std::array<uint8_t, 256> subst6;
std::array<uint8_t, 256> subst7;

int findIndex(uint8_t searchVal, std::array<uint8_t, 256> lookupArr) {
    int j = 0;
    while (j < 256) {
        if (lookupArr[j] == searchVal) {
            break;
        }
        j++;
    }
    return j;
}

std::array<uint8_t, 8> Encrypt(std::array<uint8_t, 8> inputBlock, std::array<uint8_t, 8> key) {
    int rounds = 16;
    
    for (int i = 0; i < rounds; i++) {
        for (int j = 0; j < inputBlock.size(); j++) {
            inputBlock[j] = inputBlock[j] xor key[j];
        }
        inputBlock[0] = subst0[inputBlock[0]];
        inputBlock[1] = subst1[inputBlock[1]];
        inputBlock[2] = subst2[inputBlock[2]];
        inputBlock[3] = subst3[inputBlock[3]];
        inputBlock[4] = subst4[inputBlock[4]];
        inputBlock[5] = subst5[inputBlock[5]];
        inputBlock[6] = subst6[inputBlock[6]];
        inputBlock[7] = subst7[inputBlock[7]];
        
        uint8_t tempBlock0 = inputBlock[0];
        inputBlock[0] = inputBlock[0] << 1 | (inputBlock[1] >> 7);
        inputBlock[1] = inputBlock[1] << 1 | (inputBlock[2] >> 7);
        inputBlock[2] = inputBlock[2] << 1 | (inputBlock[3] >> 7);
        inputBlock[3] = inputBlock[3] << 1 | (inputBlock[4] >> 7);
        inputBlock[4] = inputBlock[4] << 1 | (inputBlock[5] >> 7);
        inputBlock[5] = inputBlock[5] << 1 | (inputBlock[6] >> 7);
        inputBlock[6] = inputBlock[6] << 1 | (inputBlock[7] >> 7);
        inputBlock[7] = inputBlock[7] << 1 | (tempBlock0 >> 7);
    }
    
    return inputBlock;
}

std::array<uint8_t, 8> Decrypt(std::array<uint8_t, 8> inputBlock, std::array<uint8_t, 8> key) {
    int rounds = 16;
    
    for (int i = 0; i < rounds; i++) {
        uint8_t tempBlock7 = inputBlock[7];
        inputBlock[7] = inputBlock[7] >> 1 | (inputBlock[6] << 7);
        inputBlock[6] = inputBlock[6] >> 1 | (inputBlock[5] << 7);
        inputBlock[5] = inputBlock[5] >> 1 | (inputBlock[4] << 7);
        inputBlock[4] = inputBlock[4] >> 1 | (inputBlock[3] << 7);
        inputBlock[3] = inputBlock[3] >> 1 | (inputBlock[2] << 7);
        inputBlock[2] = inputBlock[2] >> 1 | (inputBlock[1] << 7);
        inputBlock[1] = inputBlock[1] >> 1 | (inputBlock[0] << 7);
        inputBlock[0] = inputBlock[0] >> 1 | (tempBlock7 << 7);
        
        inputBlock[0] = findIndex(inputBlock[0], subst0);
        inputBlock[1] = findIndex(inputBlock[1], subst1);
        inputBlock[2] = findIndex(inputBlock[2], subst2);
        inputBlock[3] = findIndex(inputBlock[3], subst3);
        inputBlock[4] = findIndex(inputBlock[4], subst4);
        inputBlock[5] = findIndex(inputBlock[5], subst5);
        inputBlock[6] = findIndex(inputBlock[6], subst6);
        inputBlock[7] = findIndex(inputBlock[7], subst7);
        
        for (int j = 0; j < inputBlock.size(); j++) {
            inputBlock[j] = inputBlock[j] xor key[j];
        }
    }
    
    return inputBlock;
}

void GenerateSubstitutionTables() {
    unsigned seed = std::chrono::system_clock::now().time_since_epoch().count();
    
    for (int i = 0; i < subst0.size(); i++) {
        subst0[i] = i;
    }
    
    subst1 = subst0;
    shuffle (subst1.begin(), subst1.end(), std::default_random_engine(seed));

    subst2 = subst1;
    shuffle (subst2.begin(), subst2.end(), std::default_random_engine(seed));

    subst3 = subst2;
    shuffle (subst3.begin(), subst3.end(), std::default_random_engine(seed));

    subst4 = subst3;
    shuffle (subst4.begin(), subst4.end(), std::default_random_engine(seed));

    subst5 = subst4;
    shuffle (subst5.begin(), subst5.end(), std::default_random_engine(seed));

    subst6 = subst5;
    shuffle (subst6.begin(), subst6.end(), std::default_random_engine(seed));

    subst7 = subst6;
    shuffle (subst7.begin(), subst7.end(), std::default_random_engine(seed));
}

std::array<uint8_t, 8> GenerateKey(std::string password) {
    std::array<uint8_t, 8> key;
    key.fill(0);
    
    for (int i = 0; i < password.length(); i++) {
        key[i % 8] = key[i % 8] xor password[i];
    }
    return key;
}

int main() {
    std::string password = "helloWorld";
    std::array<uint8_t, 8> msg = {1, 2, 3, 4, 5, 6, 7, 8};
    std::array<uint8_t, 8> key = GenerateKey(password);

    GenerateSubstitutionTables();
    
    std::array<uint8_t, 8> encryptMsg = Encrypt(msg, key);
    std::array<uint8_t, 8> encryptMsg1;
    
    std::copy(std::begin(encryptMsg), std::end(encryptMsg), std::begin(encryptMsg1));
    encryptMsg1[5] = (encryptMsg1[5] + 1) % 256;
    
    std::array<uint8_t, 8> decryptMsg = Decrypt(encryptMsg, key);
    std::array<uint8_t, 8> decryptMsg1 = Decrypt(encryptMsg1, key);
    
    if (msg == decryptMsg) {
        std::cout << "Encryption and Decryption successful for first test\n";
    }
    else {
        std::cout << "Encryption and Decryption failed\n";
    }
    
    if (msg != decryptMsg1) {
        std::cout << "Encryption and Decryption successful but the results does not match\n";
        std::cout << "Original Message: ";
        for (int i = 0; i < 8; i++) {
            std::cout << +msg[i] << " ";
        }
        std::cout << "Manipulated Message: ";
        for (int i = 0; i < 8; i++) {
            std::cout << +decryptMsg1[i] << " ";
        }
    }
    else {
        std::cout << "Encryption and Decryption failed\n";
    }
}
